package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.controller.InvoiceController;
import com.abc.SpringBootSecqurityEx.dtos.CheckoutRequest;
import com.abc.SpringBootSecqurityEx.dtos.CheckoutResponse;
import com.abc.SpringBootSecqurityEx.dtos.OrderReportRow;
import com.abc.SpringBootSecqurityEx.dtos.OrderReportRowDto;
import com.abc.SpringBootSecqurityEx.entity.User;
import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import com.abc.SpringBootSecqurityEx.entity.customer.CustomerAddress;
import com.abc.SpringBootSecqurityEx.entity.order.Invoice;
import com.abc.SpringBootSecqurityEx.entity.order.InvoiceItem;
import com.abc.SpringBootSecqurityEx.entity.order.CheckoutIdempotency;
import com.abc.SpringBootSecqurityEx.entity.order.Order;
import com.abc.SpringBootSecqurityEx.entity.order.OrderItem;
import com.abc.SpringBootSecqurityEx.entity.payment.Payment;
import com.abc.SpringBootSecqurityEx.entity.product.Product;
import com.abc.SpringBootSecqurityEx.enums.ERole;
import com.abc.SpringBootSecqurityEx.repository.CartRepository;
import com.abc.SpringBootSecqurityEx.repository.CheckoutIdempotencyRepository;
import com.abc.SpringBootSecqurityEx.repository.CustomerRepository;
import com.abc.SpringBootSecqurityEx.repository.InvoiceRepository;
import com.abc.SpringBootSecqurityEx.repository.OrderRepository;
import com.abc.SpringBootSecqurityEx.repository.PaymentRepository;
import com.abc.SpringBootSecqurityEx.repository.ProductCatalogRepository;
import com.abc.SpringBootSecqurityEx.repository.UserRepository;
import com.abc.SpringBootSecqurityEx.repository.VendorOrderRepository;
import com.abc.SpringBootSecqurityEx.util.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceFeatureTest {
    @Test
    void successfulCheckoutPersistsInvoiceFromOrderAndPendingCodPayment() {
        User user = new User();
        user.setUserName("customer-a");
        user.setRoles(EnumSet.of(ERole.CUSTOMER));
        Customer customer = new Customer();
        customer.setId(5L);
        customer.setUser(user);
        List<Payment> savedPayments = new ArrayList<>();
        List<Object> invoiceCall = new ArrayList<>();
        List<CheckoutIdempotency> savedKeys = new ArrayList<>();
        Product product = product();

        UserRepository users = fake(UserRepository.class, (method, args) -> method.equals("findByUserName")
                ? Optional.of(user) : null);
        CustomerRepository customers = fake(CustomerRepository.class, (method, args) -> switch (method) {
            case "findByUser_UserName" -> Optional.of(customer);
            case "save" -> args[0];
            default -> null;
        });
        CartRepository carts = fake(CartRepository.class, (method, args) -> switch (method) {
            case "findFirstByCustomer_User_UserNameAndStatusOrderByIdAsc" -> Optional.empty();
            case "save" -> args[0];
            default -> null;
        });
        ProductCatalogRepository products = fake(ProductCatalogRepository.class, (method, args) ->
                method.equals("findByIdForUpdate") ? Optional.of(product) : null);
        OrderRepository orders = fake(OrderRepository.class, (method, args) -> {
            if (method.equals("saveAndFlush")) {
                Order order = (Order) args[0];
                order.setId(11L);
                return order;
            }
            return null;
        });
        PaymentRepository payments = fake(PaymentRepository.class, (method, args) -> {
            if (method.equals("save")) savedPayments.add((Payment) args[0]);
            return method.equals("save") ? args[0] : null;
        });
        EntityManager entityManager = fake(EntityManager.class, (method, args) -> null);
        VendorOrderRepository vendorOrders = fake(VendorOrderRepository.class,
                (method, args) -> method.equals("saveAll") ? List.of() : null);
        InvoiceService invoices = new InvoiceService(null) {
            @Override
            public Invoice createForCheckout(Order order, Payment payment, CustomerAddress address) {
                invoiceCall.add(order);
                invoiceCall.add(payment);
                invoiceCall.add(address);
                return new Invoice();
            }
        };
        CheckoutIdempotencyRepository idempotency = fake(CheckoutIdempotencyRepository.class,
                (method, args) -> method.equals("findByCustomer_IdAndIdempotencyKey")
                        ? Optional.empty() : saveIdempotency(savedKeys, args[0]));

        CheckoutRequest request = checkoutRequest();
        OrderService service = new OrderService(orders, entityManager, users, customers, carts, products,
                vendorOrders, payments, invoices, idempotency, new ObjectMapper());
        service.checkout("customer-a", UUID.fromString("2eb9c542-efef-40dc-8a77-66c17b5c3900"), request);

        assertEquals(1, savedPayments.size());
        assertEquals("COD", savedPayments.get(0).getPaymentMethod());
        assertEquals("PENDING", savedPayments.get(0).getStatus());
        assertEquals(3, invoiceCall.size());
        Order savedOrder = (Order) invoiceCall.get(0);
        assertEquals("customer-a", savedOrder.getCustomer().getUser().getUserName());
        assertEquals("Persisted Item", savedOrder.getItems().get(0).getProduct().getName());
        assertEquals("12 Main St", ((CustomerAddress) invoiceCall.get(2)).getAddressLine1());
        assertEquals(1, savedKeys.size());
        assertEquals(savedPayments.get(0).getOrder(), savedKeys.get(0).getOrder());
        assertTrue(savedKeys.get(0).getResponseJson().contains(savedKeys.get(0).getOrder().getOrderNumber()));
    }

    @Test
    void idempotencyReturnsOriginalResponseAndRejectsDifferentRequest() throws Exception {
        User user = new User();
        user.setUserName("customer-a");
        user.setRoles(EnumSet.of(ERole.CUSTOMER));
        Customer customer = new Customer();
        customer.setId(5L);
        customer.setUser(user);
        CheckoutRequest request = checkoutRequest();
        CheckoutResponse original = new CheckoutResponse();
        original.setId(41L);
        original.setOrderNumber("ORD-ORIGINAL");
        CheckoutIdempotency previous = new CheckoutIdempotency();
        previous.setCustomer(customer);
        previous.setIdempotencyKey("2eb9c542-efef-40dc-8a77-66c17b5c3900");
        previous.setRequestHash(OrderService.checkoutRequestHash(request));
        previous.setResponseJson("{\"id\":41,\"orderNumber\":\"ORD-ORIGINAL\"}");

        UserRepository users = fake(UserRepository.class, (method, args) -> Optional.of(user));
        CustomerRepository customers = fake(CustomerRepository.class,
                (method, args) -> method.equals("findByUser_UserName") ? Optional.of(customer) : null);
        CheckoutIdempotencyRepository keys = fake(CheckoutIdempotencyRepository.class,
                (method, args) -> Optional.of(previous));
        OrderService service = new OrderService(null, fake(EntityManager.class, (method, args) -> null),
                users, customers, null, null, null, null, null, keys, new ObjectMapper());
        UUID key = UUID.fromString(previous.getIdempotencyKey());

        CheckoutResponse replay = service.checkout("customer-a", key, request);
        assertEquals(41L, replay.getId());
        assertEquals("ORD-ORIGINAL", replay.getOrderNumber());

        request.setCustomerName("Different buyer");
        var conflict = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.checkout("customer-a", key, request));
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, conflict.getStatusCode());
    }

    @Test
    void invoiceSnapshotsPersistedValuesAndRetryDoesNotCreateAnother() {
        Invoice existing = new Invoice();
        existing.setInvoiceNumber("INV-ORD-ABC");
        int[] saves = {0};
        InvoiceRepository repository = fake(InvoiceRepository.class, (method, args) -> switch (method) {
            case "findByOrder_Id" -> Optional.of(existing);
            case "save" -> { saves[0]++; yield args[0]; }
            default -> null;
        });
        InvoiceService service = new InvoiceService(repository);

        assertSame(existing, service.createForCheckout(order(11L, "ORD-ABC"), payment("PENDING"), address()));
        assertEquals(0, saves[0]);

        InvoiceRepository newRepository = fake(InvoiceRepository.class, (method, args) -> switch (method) {
            case "findByOrder_Id" -> Optional.empty();
            case "save" -> args[0];
            default -> null;
        });
        Invoice invoice = new InvoiceService(newRepository).createForCheckout(
                order(11L, "ORD-ABC"), payment("PENDING"), address());
        assertEquals("INV-ORD-ABC", invoice.getInvoiceNumber());
        assertEquals("PENDING", invoice.getPaymentStatus());
        assertEquals("Buyer", invoice.getCustomerName());
        assertEquals("12 Main St, Dhaka, 1200, Bangladesh", invoice.getDeliveryAddress());
        assertEquals(new BigDecimal("200.00"), invoice.getGrandTotal());
        assertEquals("Persisted Item", invoice.getItems().get(0).getItemName());
        assertEquals(new BigDecimal("100.00"), invoice.getItems().get(0).getLineTotal());
    }

    @Test
    void invoiceSnapshotAcceptsCheckoutMaximumCustomerNameLength() {
        Order order = order(12L, "ORD-LONG-NAME");
        order.getCustomer().setFullName("N".repeat(255));
        InvoiceRepository repository = fake(InvoiceRepository.class, (method, args) -> switch (method) {
            case "findByOrder_Id" -> Optional.empty();
            case "save" -> args[0];
            default -> null;
        });

        Invoice invoice = new InvoiceService(repository).createForCheckout(order, payment("PENDING"), address());

        assertEquals(255, invoice.getCustomerName().length());
    }

    @Test
    void customerInvoiceOwnershipAndMissingInvoiceReturnNotFound() {
        Invoice invoice = invoiceFor("customer-a");
        InvoiceRepository repository = fake(InvoiceRepository.class, (method, args) ->
                method.equals("findByOrder_Id") && args[0].equals(11L) ? Optional.of(invoice) : Optional.empty());
        InvoiceService service = new InvoiceService(repository);
        assertThrows(NotFoundException.class, () -> service.getCustomerInvoicePdf(11L, "customer-b"));
        assertThrows(NotFoundException.class, () -> service.getCustomerInvoicePdf(12L, "customer-a"));
    }

    @Test
    void pdfEndpointReturnsApplicationPdfWithSafeAttachmentName() {
        InvoiceService service = new InvoiceService(null) {
            @Override
            public InvoicePdf getCustomerInvoicePdf(Long id, String username) {
                return new InvoicePdf(new byte[]{1, 2, 3}, "INV-ORD/ABC");
            }
        };
        var response = new InvoiceController(service).customerInvoice(11L,
                UsernamePasswordAuthenticationToken.authenticated("customer-a", "", List.of()));
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("INV-ORD_ABC.pdf"));
        assertArrayEquals(new byte[]{1, 2, 3}, response.getBody());
    }

    @Test
    void reportDateBoundsAreInclusiveAndInvalidRangesAreRejected() {
        Object[][] captured = {null};
        OrderRepository repository = fake(OrderRepository.class, (method, args) -> {
            if (!method.equals("searchReport")) return null;
            captured[0] = args;
            return new PageImpl<>(List.<OrderReportRow>of(), (Pageable) args[4], 0);
        });
        OrderReportService service = new OrderReportService(repository);
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 4);
        Page<OrderReportRow> rows = service.findOrders(start, end, " pending ", " AbC ", 0, 10);
        assertTrue(rows.isEmpty());
        assertEquals(start.atStartOfDay(), captured[0][0]);
        assertEquals(end.plusDays(1).atStartOfDay(), captured[0][1]);
        assertEquals("pending", captured[0][2]);
        assertEquals("AbC", captured[0][3]);

        service.findOrders(null, null, null, "search", 0, 10);
        assertNull(captured[0][0]);
        assertNull(captured[0][1]);
        assertNull(captured[0][2]);
        assertEquals("search", captured[0][3]);

        service.findOrders(start, null, null, null, 2, 7);
        assertEquals(start.atStartOfDay(), captured[0][0]);
        assertNull(captured[0][1]);
        assertNull(captured[0][2]);
        assertNull(captured[0][3]);
        assertEquals(2, ((Pageable) captured[0][4]).getPageNumber());
        assertEquals(7, ((Pageable) captured[0][4]).getPageSize());

        service.findOrders(null, end, null, null, 0, 10);
        assertNull(captured[0][0]);
        assertEquals(end.plusDays(1).atStartOfDay(), captured[0][1]);

        service.findOrders(start, end, null, null, 0, 10);
        assertEquals(start.atStartOfDay(), captured[0][0]);
        assertEquals(end.plusDays(1).atStartOfDay(), captured[0][1]);
        assertNull(captured[0][2]);
        assertNull(captured[0][3]);

        service.findOrders(null, null, "PAID", null, 0, 10);
        assertEquals("PAID", captured[0][2]);
        assertNull(captured[0][3]);

        service.findOrders(null, null, null, null, 0, 10);
        assertNull(captured[0][0]);
        assertNull(captured[0][1]);
        assertNull(captured[0][2]);
        assertNull(captured[0][3]);

        service.findOrders(start, end, "pending", "Ada", 1, 5);
        assertEquals(start.atStartOfDay(), captured[0][0]);
        assertEquals(end.plusDays(1).atStartOfDay(), captured[0][1]);
        assertEquals("pending", captured[0][2]);
        assertEquals("Ada", captured[0][3]);
        assertEquals(1, ((Pageable) captured[0][4]).getPageNumber());
        assertEquals(5, ((Pageable) captured[0][4]).getPageSize());
        assertThrows(ResponseStatusException.class, () -> service.findOrders(end, start, null, null, 0, 10));
    }

    @Test
    void tupleProjectionMapsToScalarReportDtoAndSerializesWithoutJpaObjects() throws Exception {
        LocalDateTime orderDate = LocalDateTime.of(2026, 11, 4, 10, 15);
        Map<String, Object> tupleValues = Map.of(
                "id", 52L,
                "orderNumber", "ORD-52",
                "customerName", "Ada",
                "orderDate", orderDate,
                "total", new BigDecimal("42.50"),
                "orderStatus", "DELIVERED",
                "paymentStatus", "PAID",
                "invoiceAvailable", true);
        Tuple tuple = fake(Tuple.class, (method, args) -> method.equals("get")
                ? tupleValues.get(args[0]) : null);

        OrderReportRowDto dto = OrderReportRowDto.from(tuple);

        assertEquals(52L, dto.getId());
        assertEquals("PAID", dto.getPaymentStatus());
        assertEquals(true, dto.getInvoiceAvailable());
        assertEquals(orderDate, dto.getOrderDate());
        String json = new ObjectMapper().writeValueAsString(dto);
        assertTrue(json.contains("\"orderNumber\":\"ORD-52\""));
        assertTrue(json.contains("\"invoiceAvailable\":true"));
        assertFalse(json.contains("tuple"));
    }

    @Test
    void jasperInvoiceRendererProducesPdfBytes() {
        Invoice invoice = invoiceFor("customer-a");
        invoice.setInvoiceNumber("INV-ORD-ABC");
        invoice.setCreatedAt(LocalDateTime.of(2026, 11, 4, 10, 0));
        invoice.setCustomerName("Buyer");
        invoice.setCustomerPhone("555-0100");
        invoice.setDeliveryAddress("12 Main St, Dhaka");
        invoice.setPaymentMethod("COD");
        invoice.setPaymentStatus("PENDING");
        invoice.setOrderStatus("PENDING");
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setDiscount(BigDecimal.ZERO);
        invoice.setShippingFee(new BigDecimal("100.00"));
        invoice.setGrandTotal(new BigDecimal("200.00"));
        InvoiceItem line = new InvoiceItem();
        line.setInvoice(invoice);
        line.setItemName("Persisted Item");
        line.setQuantity(2);
        line.setUnitPrice(new BigDecimal("50.00"));
        line.setLineTotal(new BigDecimal("100.00"));
        invoice.setItems(List.of(line));
        InvoiceRepository repository = fake(InvoiceRepository.class, (method, args) ->
                method.equals("findByOrder_Id") ? Optional.of(invoice) : null);

        byte[] pdf = new InvoiceService(repository).getInvoicePdf(11L).content();
        assertTrue(pdf.length > 4);
        assertEquals("%PDF", new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
    }

    private static <T> T fake(Class<T> type, MethodHandler handler) {
        InvocationHandler invocationHandler = (proxy, method, args) -> handler.invoke(method.getName(), args == null ? new Object[0] : args);
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, invocationHandler));
    }

    private static Object saveIdempotency(List<CheckoutIdempotency> saved, Object entity) {
        saved.add((CheckoutIdempotency) entity);
        return entity;
    }

    private static CheckoutRequest checkoutRequest() {
        CheckoutRequest request = new CheckoutRequest();
        request.setCustomerName("Buyer"); request.setPhone("555-0100"); request.setAddress("12 Main St");
        request.setCity("Dhaka"); request.setPostalCode("1200"); request.setPaymentMethod("COD");
        CheckoutRequest.CheckoutItem item = new CheckoutRequest.CheckoutItem();
        item.setProductId(8L); item.setQuantity(2); request.setItems(List.of(item));
        return request;
    }

    private static Product product() {
        Product product = new Product(); product.setId(8L); product.setName("Persisted Item");
        product.setActive(true); product.setStockQuantity(4); product.setBasePrice(new BigDecimal("75.00"));
        return product;
    }

    private static Invoice invoiceFor(String username) {
        User user = new User(); user.setUserName(username);
        Customer customer = new Customer(); customer.setUser(user);
        Order order = new Order(); order.setCustomer(customer); order.setOrderNumber("ORD-ABC");
        Invoice invoice = new Invoice(); invoice.setOrder(order); invoice.setInvoiceNumber("INV-ORD-ABC");
        return invoice;
    }

    private static Order order(Long id, String orderNumber) {
        User user = new User(); user.setUserName("customer-a");
        Customer customer = new Customer(); customer.setUser(user); customer.setFullName("Buyer"); customer.setPhone("555-0100");
        Order order = new Order(); order.setId(id); order.setOrderNumber(orderNumber); order.setCustomer(customer);
        order.setStatus("PENDING"); order.setSubtotal(new BigDecimal("100.00")); order.setDiscount(BigDecimal.ZERO);
        order.setShippingFee(new BigDecimal("100.00")); order.setTotalAmount(new BigDecimal("200.00"));
        order.setCreatedAt(LocalDateTime.of(2026, 11, 4, 10, 0));
        OrderItem item = new OrderItem(); item.setOrder(order); item.setProduct(product()); item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("50.00")); item.setTotalPrice(new BigDecimal("100.00"));
        order.setItems(new ArrayList<>(List.of(item)));
        return order;
    }

    private static Payment payment(String status) {
        Payment payment = new Payment(); payment.setPaymentMethod("COD"); payment.setStatus(status); return payment;
    }

    private static CustomerAddress address() {
        CustomerAddress address = new CustomerAddress(); address.setAddressLine1("12 Main St");
        address.setCity("Dhaka"); address.setState("Dhaka"); address.setPostalCode("1200");
        address.setCountry("Bangladesh"); return address;
    }

    @FunctionalInterface
    private interface MethodHandler { Object invoke(String method, Object[] args) throws Throwable; }
}
