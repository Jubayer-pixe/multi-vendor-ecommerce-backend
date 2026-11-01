package com.abc.SpringBootSecqurityEx.entity.vendor;

import com.abc.SpringBootSecqurityEx.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@NoArgsConstructor
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String contactPerson;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Boolean verified = false;

    @Column(precision = 10, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorAddress> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorBankAccount> bankAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorDocument> documents = new ArrayList<>();

    @OneToOne(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    private VendorWallet wallet;
}

