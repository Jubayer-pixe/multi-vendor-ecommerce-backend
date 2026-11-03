package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.VendorRegistrationDecisionDTO;
import com.abc.SpringBootSecqurityEx.dtos.VendorRegistrationRequestDTO;
import com.abc.SpringBootSecqurityEx.entity.User;
import com.abc.SpringBootSecqurityEx.entity.vendor.Vendor;
import com.abc.SpringBootSecqurityEx.entity.vendor.VendorRegistrationRequest;
import com.abc.SpringBootSecqurityEx.enums.ERole;
import com.abc.SpringBootSecqurityEx.enums.VendorRegistrationStatus;
import com.abc.SpringBootSecqurityEx.repository.UserRepository;
import com.abc.SpringBootSecqurityEx.repository.VendorRegistrationRequestRepository;
import com.abc.SpringBootSecqurityEx.repository.VendorRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class VendorRegistrationRequestService {

    private final VendorRegistrationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;

    public VendorRegistrationRequestService(
            VendorRegistrationRequestRepository requestRepository,
            UserRepository userRepository,
            VendorRepository vendorRepository
    ) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
    }

    public VendorRegistrationRequestDTO submitRequest(String username, VendorRegistrationRequestDTO dto) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (vendorRepository.existsByUser_UserName(user.getUserName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A vendor profile already exists for this user");
        }

        if (requestRepository.existsByUser_UserNameAndStatusNot(user.getUserName(), VendorRegistrationStatus.REJECTED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "There is already a pending or approved vendor request for this user");
        }

        if (requestRepository.existsByBusinessEmailAndStatus(dto.getBusinessEmail(), VendorRegistrationStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This business email already has a pending vendor request");
        }

        VendorRegistrationRequest request = new VendorRegistrationRequest();
        request.setUser(user);
        request.setBusinessName(dto.getBusinessName());
        request.setContactPerson(dto.getContactPerson());
        request.setBusinessEmail(dto.getBusinessEmail());
        request.setBusinessPhone(dto.getBusinessPhone());
        request.setBusinessAddress(dto.getBusinessAddress());
        request.setBusinessDescription(dto.getBusinessDescription());
        request.setTradeLicenseNumber(dto.getTradeLicenseNumber());
        request.setStatus(VendorRegistrationStatus.PENDING);

        VendorRegistrationRequest saved = requestRepository.save(request);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<VendorRegistrationRequestDTO> findAll() {
        return requestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VendorRegistrationRequestDTO> findByStatus(VendorRegistrationStatus status) {
        return requestRepository.findAllByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public VendorRegistrationRequestDTO findById(Long id) {
        return toDto(requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vendor request not found")));
    }

    @Transactional(readOnly = true)
    public VendorRegistrationRequestDTO findMyRequest(String username) {
        VendorRegistrationRequest request = requestRepository.findTopByUser_UserNameOrderByCreatedAtDesc(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No vendor request found"));

        return toDto(request);
    }

    public VendorRegistrationRequestDTO approve(Long id, String reviewerUsername) {
        VendorRegistrationRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor request not found"));

        if (request.getStatus() != VendorRegistrationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request is not pending");
        }

        User user = request.getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request user missing");
        }

        if (vendorRepository.existsByUser_UserName(user.getUserName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A vendor profile already exists for this user");
        }

        if (vendorRepository.existsByEmail(request.getBusinessEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vendor email is already in use");
        }

        user.getRoles().add(ERole.VENDOR);
        userRepository.save(user);

        Vendor vendor = new Vendor();
        vendor.setUser(user);
        vendor.setBusinessName(request.getBusinessName());
        vendor.setContactPerson(request.getContactPerson());
        vendor.setPhone(request.getBusinessPhone());
        vendor.setEmail(request.getBusinessEmail());
        vendor.setVerified(false);
        vendor.setCreatedAt(LocalDateTime.now());

        vendorRepository.save(vendor);

        request.setStatus(VendorRegistrationStatus.APPROVED);
        request.setReviewedBy(reviewerUsername);
        request.setReviewedAt(LocalDateTime.now());
        request.setRejectionReason(null);
        request.setUpdatedAt(LocalDateTime.now());

        return toDto(requestRepository.save(request));
    }

    public VendorRegistrationRequestDTO reject(Long id, String reviewerUsername, VendorRegistrationDecisionDTO dto) {
        VendorRegistrationRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor request not found"));

        if (request.getStatus() != VendorRegistrationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request is not pending");
        }

        request.setStatus(VendorRegistrationStatus.REJECTED);
        request.setReviewedBy(reviewerUsername);
        request.setReviewedAt(LocalDateTime.now());
        request.setRejectionReason(dto.getReason());
        request.setUpdatedAt(LocalDateTime.now());

        return toDto(requestRepository.save(request));
    }

    private VendorRegistrationRequestDTO toDto(VendorRegistrationRequest request) {
        VendorRegistrationRequestDTO dto = new VendorRegistrationRequestDTO();
        dto.setId(request.getId());
        dto.setUserName(request.getUser() == null ? null : request.getUser().getUserName());
        dto.setBusinessName(request.getBusinessName());
        dto.setContactPerson(request.getContactPerson());
        dto.setBusinessEmail(request.getBusinessEmail());
        dto.setBusinessPhone(request.getBusinessPhone());
        dto.setBusinessAddress(request.getBusinessAddress());
        dto.setBusinessDescription(request.getBusinessDescription());
        dto.setTradeLicenseNumber(request.getTradeLicenseNumber());
        dto.setStatus(request.getStatus());
        dto.setRejectionReason(request.getRejectionReason());
        dto.setReviewedBy(request.getReviewedBy());
        dto.setReviewedAt(request.getReviewedAt());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());
        return dto;
    }
}