package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.vendor.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    long countByVerifiedFalse();

    Optional<Vendor> findByUser_UserName(String username);

    boolean existsByUser_UserName(String username);

    boolean existsByUser_UserNameAndIdNot(String username, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
