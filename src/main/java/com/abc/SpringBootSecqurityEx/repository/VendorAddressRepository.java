package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.vendor.VendorAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorAddressRepository extends JpaRepository<VendorAddress, Long> {
}
