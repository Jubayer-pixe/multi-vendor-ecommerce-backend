package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.vendor.VendorWallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorWalletRepository extends JpaRepository<VendorWallet, Long> {
}
