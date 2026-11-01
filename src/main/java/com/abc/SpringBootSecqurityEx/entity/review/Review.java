package com.abc.SpringBootSecqurityEx.entity.review;

import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import com.abc.SpringBootSecqurityEx.entity.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Integer rating;

    @Column
    private String title;

    @Column(length = 2000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

