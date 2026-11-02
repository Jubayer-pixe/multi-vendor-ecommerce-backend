package com.abc.SpringBootSecqurityEx.entity.order;

import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import com.abc.SpringBootSecqurityEx.entity.delivery.Delivery;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(length = 1000)
    private String customerNote;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    private Delivery delivery;
}
