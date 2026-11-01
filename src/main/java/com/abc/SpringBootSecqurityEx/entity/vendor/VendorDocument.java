package com.abc.SpringBootSecqurityEx.entity.vendor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vendor_documents")
@Getter
@Setter
@NoArgsConstructor
public class VendorDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false)
    private String documentType;

    @Column(nullable = false)
    private String documentNumber;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private String status = "PENDING";
}
