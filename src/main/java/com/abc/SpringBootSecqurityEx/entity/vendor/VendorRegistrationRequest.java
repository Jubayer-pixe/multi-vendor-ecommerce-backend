package com.abc.SpringBootSecqurityEx.entity.vendor;

import com.abc.SpringBootSecqurityEx.entity.User;
import com.abc.SpringBootSecqurityEx.enums.VendorRegistrationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_registration_requests")
@Getter
@Setter
@NoArgsConstructor
public class VendorRegistrationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_name", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String businessName;

    @Column(nullable = false, length = 255)
    private String contactPerson;

    @Column(nullable = false, length = 255)
    private String businessEmail;

    @Column(length = 30)
    private String businessPhone;

    @Column(length = 500)
    private String businessAddress;

    @Column(length = 2000)
    private String businessDescription;

    @Column(length = 255)
    private String tradeLicenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorRegistrationStatus status = VendorRegistrationStatus.PENDING;

    @Column(length = 1000)
    private String rejectionReason;

    @Column(length = 255)
    private String reviewedBy;

    @Column
    private LocalDateTime reviewedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}