package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.vendor.VendorDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorDocumentRepository extends JpaRepository<VendorDocument, Long> {
}
