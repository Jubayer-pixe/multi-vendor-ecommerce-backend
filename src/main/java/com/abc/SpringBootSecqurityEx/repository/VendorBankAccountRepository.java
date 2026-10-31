package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.vendor.VendorBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorBankAccountRepository extends JpaRepository<VendorBankAccount, Long> {
}
