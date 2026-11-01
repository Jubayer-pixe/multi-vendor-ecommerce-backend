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
import com.abc.SpringBootSecqurityEx.repository.OrderRepository;
import com.abc.SpringBootSecqurityEx.repository.PaymentRepository;
import com.abc.SpringBootSecqurityEx.repository.ProductCatalogRepository;
import com.abc.SpringBootSecqurityEx.repository.UserRepository;
import com.abc.SpringBootSecqurityEx.repository.VendorOrderRepository;
import com.abc.SpringBootSecqurityEx.util.NotFoundException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.UUID;
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

    public OrderService(OrderRepository repository, EntityManager entityManager, UserRepository userRepository,
                        CustomerRepository customerRepository, CartRepository cartRepository,
                        ProductCatalogRepository productRepository, VendorOrderRepository vendorOrderRepository,
                        PaymentRepository paymentRepository) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.vendorOrderRepository = vendorOrderRepository;
        this.paymentRepository = paymentRepository;
    }

    public CheckoutResponse checkout(String username, CheckoutRequest request) {
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

        Customer customer = customerRepository.findByUser_UserName(username).orElseGet(() -> {
            Customer created = new Customer();
            created.setUser(user);
            created.setLoyaltyPoints(0);
            created.setFullName(request.getCustomerName().trim());
            created.setPhone(request.getPhone().trim());
            return created;
        });
        customer.setFullName(request.getCustomerName().trim());
        customer.setPhone(request.getPhone().trim());
        saveDeliveryAddress(customer, request);

        Map<Long, Integer> requestedQuantities = new LinkedHashMap<>();
        for (CheckoutRequest.CheckoutItem line : request.getItems()) {
            if (requestedQuantities.putIfAbsent(line.getProductId(), line.getQuantity()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart contains duplicate product lines");
            }
        }
        if (requestedQuantities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }

        Cart cart = cartRepository.findFirstByCustomer_User_UserNameAndStatusOrderByIdAsc(username, "ACTIVE")
                .orElseGet(() -> {
                    Cart created = new Cart();
                    created.setCustomer(customer);
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
        return response;
    }

    private void saveDeliveryAddress(Customer customer, CheckoutRequest request) {
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
