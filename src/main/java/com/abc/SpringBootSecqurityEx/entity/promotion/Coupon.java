package com.abc.SpringBootSecqurityEx.entity.promotion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String discountType;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(precision = 19, scale = 2)
    private BigDecimal minOrderValue;

    @Column(precision = 19, scale = 2)
    private BigDecimal maxDiscount;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validTo;

    @Column(nullable = false)
    private Boolean active = true;
}

