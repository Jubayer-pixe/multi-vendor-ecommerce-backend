package com.abc.SpringBootSecqurityEx.entity.vendor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vendor_bank_accounts")
@Getter
@Setter
@NoArgsConstructor
public class VendorBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String ifscCode;

    @Column(nullable = false)
    private Boolean primaryAccount = false;
}
