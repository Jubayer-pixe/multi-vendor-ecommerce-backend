package com.abc.SpringBootSecqurityEx.entity.commission;

import com.abc.SpringBootSecqurityEx.entity.order.Order;
import com.abc.SpringBootSecqurityEx.entity.vendor.Vendor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commissions")
@Getter
@Setter
@NoArgsConstructor
public class Commission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal rate;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

