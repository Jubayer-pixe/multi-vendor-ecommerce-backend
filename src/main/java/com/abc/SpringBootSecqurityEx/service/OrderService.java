package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.OrderDTO;
import com.abc.SpringBootSecqurityEx.dtos.CheckoutRequest;
import com.abc.SpringBootSecqurityEx.dtos.CheckoutResponse;
import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import com.abc.SpringBootSecqurityEx.entity.customer.CustomerAddress;
import com.abc.SpringBootSecqurityEx.entity.User;
import com.abc.SpringBootSecqurityEx.entity.shopping.Cart;
import com.abc.SpringBootSecqurityEx.entity.shopping.CartItem;
import com.abc.SpringBootSecqurityEx.entity.product.Product;
import com.abc.SpringBootSecqurityEx.entity.order.VendorOrder;
import com.abc.SpringBootSecqurityEx.entity.order.VendorOrderItem;
import com.abc.SpringBootSecqurityEx.entity.payment.Payment;
import com.abc.SpringBootSecqurityEx.enums.ERole;
import com.abc.SpringBootSecqurityEx.repository.CartRepository;
import com.abc.SpringBootSecqurityEx.repository.CustomerRepository;
import com.abc.SpringBootSecqurityEx.entity.order.Order;
import com.abc.SpringBootSecqurityEx.entity.order.OrderItem;
import com.abc.SpringBootSecqurityEx.entity.order.CheckoutIdempotency;
import com.abc.SpringBootSecqurityEx.repository.OrderRepository;
import com.abc.SpringBootSecqurityEx.repository.CheckoutIdempotencyRepository;
import com.abc.SpringBootSecqurityEx.repository.PaymentRepository;
import com.abc.SpringBootSecqurityEx.repository.ProductCatalogRepository;
import com.abc.SpringBootSecqurityEx.repository.UserRepository;
import com.abc.SpringBootSecqurityEx.repository.VendorOrderRepository;
import com.abc.SpringBootSecqurityEx.util.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.UUID;
import java.util.Comparator;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class OrderService {
    private final OrderRepository repository;
    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CartRepository cartRepository;
    private final ProductCatalogRepository productRepository;
    private final VendorOrderRepository vendorOrderRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final CheckoutIdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository repository, EntityManager entityManager, UserRepository userRepository,
                        CustomerRepository customerRepository, CartRepository cartRepository,
                        ProductCatalogRepository productRepository, VendorOrderRepository vendorOrderRepository,
                        PaymentRepository paymentRepository, InvoiceService invoiceService,
                        CheckoutIdempotencyRepository idempotencyRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.vendorOrderRepository = vendorOrderRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceService = invoiceService;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    public CheckoutResponse checkout(String username, UUID idempotencyKey, CheckoutRequest request) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in as a customer to place an order");
        }
        if (!"COD".equalsIgnoreCase(request.getPaymentMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only Cash on Delivery is available. Online payment is not configured yet.");
        }
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer account was not found"));
        if (user.getRoles() == null || !user.getRoles().contains(ERole.CUSTOMER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customer accounts can place orders");
        }
        if (idempotencyKey == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }
        // Serialize checkouts for this principal so concurrent requests with the same key
        // observe the committed idempotency row instead of racing its unique constraint.
        entityManager.lock(user, LockModeType.PESSIMISTIC_WRITE);

        Customer customer = customerRepository.findByUser_UserName(username).orElseGet(() -> {
            Customer created = new Customer();
            created.setUser(user);
            created.setLoyaltyPoints(0);
            created.setFullName(request.getCustomerName().trim());
            created.setPhone(request.getPhone().trim());
            return created;
        });
        if (customer.getId() == null) {
            customer = customerRepository.save(customer);
        }
        String requestHash = checkoutRequestHash(request);
        Optional<CheckoutIdempotency> previous = idempotencyRepository
                .findByCustomer_IdAndIdempotencyKey(customer.getId(), idempotencyKey.toString());
        if (previous.isPresent()) {
            CheckoutIdempotency prior = previous.get();
            if (!prior.getRequestHash().equals(requestHash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key was already used with a different checkout request");
            }
            try {
                return objectMapper.readValue(prior.getResponseJson(), CheckoutResponse.class);
            } catch (JacksonException exception) {
                throw new IllegalStateException("Stored checkout response could not be read", exception);
            }
        }

        customer.setFullName(request.getCustomerName().trim());
        customer.setPhone(request.getPhone().trim());
        CustomerAddress persistedAddress = saveDeliveryAddress(customer, request);

        Map<Long, Integer> requestedQuantities = new LinkedHashMap<>();
        for (CheckoutRequest.CheckoutItem line : request.getItems()) {
            if (requestedQuantities.putIfAbsent(line.getProductId(), line.getQuantity()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart contains duplicate product lines");
            }
        }
        if (requestedQuantities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }

        Customer checkoutCustomer = customer;
        Cart cart = cartRepository.findFirstByCustomer_User_UserNameAndStatusOrderByIdAsc(username, "ACTIVE")
                .orElseGet(() -> {
                    Cart created = new Cart();
                    created.setCustomer(checkoutCustomer);
                    created.setStatus("ACTIVE");
                    return created;
                });
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        order.setStatus("PENDING");
        order.setDiscount(BigDecimal.ZERO);
        order.setCustomerNote(request.getCustomerNote() == null ? null : request.getCustomerNote().trim());

        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Long, VendorOrder> vendorOrders = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> requested : requestedQuantities.entrySet()) {
            Product product = productRepository.findByIdForUpdate(requested.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Product " + requested.getKey() + " is no longer available"));
            int quantity = requested.getValue();
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, product.getName() + " is no longer available");
            }
            if (product.getStockQuantity() == null || product.getStockQuantity() < quantity) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not enough stock for " + product.getName() + ". Available: "
                                + (product.getStockQuantity() == null ? 0 : product.getStockQuantity()));
            }
            BigDecimal unitPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice();
            if (unitPrice == null || unitPrice.signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid price for " + product.getName());
            }
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setVariantId(0L);
            cartItem.setQuantity(quantity);
            cartItem.setUnitPrice(unitPrice);
            cart.getItems().add(cartItem);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setVariantId(0L);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setTotalPrice(lineTotal);
            order.getItems().add(orderItem);
            subtotal = subtotal.add(lineTotal);

            product.setStockQuantity(product.getStockQuantity() - quantity);
            if (product.getVendor() != null) {
                VendorOrder vendorOrder = vendorOrders.computeIfAbsent(product.getVendor().getId(), id -> {
                    VendorOrder created = new VendorOrder();
                    created.setOrder(order);
                    created.setVendor(product.getVendor());
                    created.setStatus("PENDING");
                    created.setSubtotal(BigDecimal.ZERO);
                    created.setShippingFee(BigDecimal.ZERO);
                    created.setCommissionAmount(BigDecimal.ZERO);
                    return created;
                });
                VendorOrderItem vendorItem = new VendorOrderItem();
                vendorItem.setVendorOrder(vendorOrder);
                vendorItem.setOrderItem(orderItem);
                vendorItem.setQuantity(quantity);
                vendorItem.setUnitPrice(unitPrice);
                vendorItem.setTotalPrice(lineTotal);
                vendorOrder.getItems().add(vendorItem);
                vendorOrder.setSubtotal(vendorOrder.getSubtotal().add(lineTotal));
            }
        }

        BigDecimal shippingFee = new BigDecimal("100.00");
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(subtotal.add(shippingFee));
        order.setCreatedAt(LocalDateTime.now());
        repository.saveAndFlush(order);

        vendorOrderRepository.saveAll(vendorOrders.values());

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod("COD");
        payment.setPaymentGateway("COD");
        payment.setAmount(order.getTotalAmount());
        payment.setStatus("PENDING");
        paymentRepository.save(payment);

        invoiceService.createForCheckout(order, payment, persistedAddress);

        cart.getItems().clear();
        cart.setStatus("CHECKED_OUT");
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        entityManager.flush();

        CheckoutResponse response = new CheckoutResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus());
        response.setSubtotal(order.getSubtotal());
        response.setShippingFee(order.getShippingFee());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());
        response.setCustomerName(customer.getFullName());
        response.setPhone(customer.getPhone());
        response.setAddress(request.getAddress().trim());
        response.setCity(request.getCity().trim());
        response.setPostalCode(request.getPostalCode().trim());
        response.setPaymentMethod("COD");
        CheckoutIdempotency idempotency = new CheckoutIdempotency();
        idempotency.setCustomer(customer);
        idempotency.setIdempotencyKey(idempotencyKey.toString());
        idempotency.setRequestHash(requestHash);
        idempotency.setOrder(order);
        idempotency.setResponseJson(serializeResponse(response));
        idempotency.setCreatedAt(LocalDateTime.now());
        idempotencyRepository.save(idempotency);
        return response;
    }

    static String checkoutRequestHash(CheckoutRequest request) {
        StringBuilder canonical = new StringBuilder();
        appendHashField(canonical, request.getCustomerName().trim());
        appendHashField(canonical, request.getPhone().trim());
        appendHashField(canonical, request.getAddress().trim());
        appendHashField(canonical, request.getCity().trim());
        appendHashField(canonical, request.getPostalCode().trim());
        appendHashField(canonical, request.getPaymentMethod().trim().toUpperCase(java.util.Locale.ROOT));
        appendHashField(canonical, request.getCustomerNote() == null ? "" : request.getCustomerNote().trim());
        request.getItems().stream()
                .sorted(Comparator.comparing(CheckoutRequest.CheckoutItem::getProductId)
                        .thenComparing(CheckoutRequest.CheckoutItem::getQuantity))
                .forEach(item -> {
                    appendHashField(canonical, item.getProductId().toString());
                    appendHashField(canonical, item.getQuantity().toString());
                });
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void appendHashField(StringBuilder target, String value) {
        target.append(value.length()).append(':').append(value).append(';');
    }

    private String serializeResponse(CheckoutResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Checkout response could not be stored", exception);
        }
    }

    private CustomerAddress saveDeliveryAddress(Customer customer, CheckoutRequest request) {
        customer.getAddresses().forEach(saved -> saved.setDefaultAddress(false));
        CustomerAddress address = new CustomerAddress();
        address.setCustomer(customer);
        address.setAddressLine1(request.getAddress().trim());
        address.setCity(request.getCity().trim());
        address.setState(request.getCity().trim());
        address.setCountry("Bangladesh");
        address.setPostalCode(request.getPostalCode().trim());
        address.setDefaultAddress(true);
        customer.getAddresses().add(address);
        customerRepository.save(customer);
        return address;
    }

    public OrderDTO save(OrderDTO dto) {
        Order entity = dto.getId() == null ? new Order() : repository.findById(dto.getId()).orElseGet(Order::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<OrderDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(OrderDTO dto, Order entity) {
        entity.setId(dto.getId());
        entity.setCustomer(dto.getCustomerId() == null ? null : entityManager.getReference(Customer.class, dto.getCustomerId()));
        entity.setOrderNumber(dto.getOrderNumber());
        entity.setStatus(dto.getStatus());
        entity.setSubtotal(dto.getSubtotal());
        entity.setShippingFee(dto.getShippingFee());
        entity.setDiscount(dto.getDiscount());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setCreatedAt(dto.getCreatedAt());
        preserveItems(dto.getItemIds(), entity);
    }

    private OrderDTO toDto(Order entity) {
        OrderDTO dto = new OrderDTO();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomer() == null ? null : entity.getCustomer().getId());
        dto.setOrderNumber(entity.getOrderNumber());
        dto.setStatus(entity.getStatus());
        dto.setSubtotal(entity.getSubtotal());
        dto.setShippingFee(entity.getShippingFee());
        dto.setDiscount(entity.getDiscount());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setItemIds(entity.getItems() == null ? List.of() : entity.getItems().stream().map(OrderItem::getId).toList());
        return dto;
    }

    private void preserveItems(List<Long> requestedIds, Order order) {
        if (requestedIds == null) return;
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate order item ids are not supported");
        }
        for (Long id : requestedIds) {
            OrderItem item = entityManager.find(OrderItem.class, id);
            if (item == null) throw new NotFoundException("Order item not found: " + id);
            if (item.getOrder() == null || order.getId() == null || !order.getId().equals(item.getOrder().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Order items cannot be reassigned through order update");
            }
        }
        List<Long> existingIds = order.getItems().stream().map(OrderItem::getId).toList();
        if (!new HashSet<>(existingIds).equals(new HashSet<>(requestedIds)) || existingIds.size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacing order items may orphan historical order items");
        }
    }
}
