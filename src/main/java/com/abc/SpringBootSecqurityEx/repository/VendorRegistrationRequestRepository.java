package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.vendor.VendorRegistrationRequest;
import com.abc.SpringBootSecqurityEx.enums.VendorRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorRegistrationRequestRepository extends JpaRepository<VendorRegistrationRequest, Long> {

    Optional<VendorRegistrationRequest> findTopByUser_UserNameOrderByCreatedAtDesc(String userName);

    List<VendorRegistrationRequest> findAllByOrderByCreatedAtDesc();

    List<VendorRegistrationRequest> findAllByStatusOrderByCreatedAtDesc(VendorRegistrationStatus status);

    boolean existsByUser_UserNameAndStatus(String userName, VendorRegistrationStatus status);

    boolean existsByBusinessEmailAndStatus(String businessEmail, VendorRegistrationStatus status);

    boolean existsByUser_UserNameAndStatusNot(String userName, VendorRegistrationStatus status);
}