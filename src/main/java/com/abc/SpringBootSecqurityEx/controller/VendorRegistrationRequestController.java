package com.abc.SpringBootSecqurityEx.controller;

import com.abc.SpringBootSecqurityEx.dtos.VendorRegistrationDecisionDTO;
import com.abc.SpringBootSecqurityEx.dtos.VendorRegistrationRequestDTO;
import com.abc.SpringBootSecqurityEx.enums.VendorRegistrationStatus;
import com.abc.SpringBootSecqurityEx.service.VendorRegistrationRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VendorRegistrationRequestController {

    private final VendorRegistrationRequestService service;

    public VendorRegistrationRequestController(VendorRegistrationRequestService service) {
        this.service = service;
    }

    @PostMapping("/vendor-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VendorRegistrationRequestDTO> submitRequest(
            Authentication authentication,
            @Valid @RequestBody VendorRegistrationRequestDTO dto
    ) {
        VendorRegistrationRequestDTO saved = service.submitRequest(authentication.getName(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/vendor-requests/my-request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VendorRegistrationRequestDTO> myRequest(Authentication authentication) {
        return ResponseEntity.ok(service.findMyRequest(authentication.getName()));
    }

    @GetMapping("/admin/vendor-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VendorRegistrationRequestDTO>> listRequests() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/admin/vendor-requests/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VendorRegistrationRequestDTO> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/admin/vendor-requests/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VendorRegistrationRequestDTO>> listByStatus(@PathVariable String status) {
        return ResponseEntity.ok(service.findByStatus(VendorRegistrationStatus.valueOf(status.toUpperCase())));
    }

    @PutMapping("/admin/vendor-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VendorRegistrationRequestDTO> approveRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.approve(id, authentication.getName()));
    }

    @PutMapping("/admin/vendor-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VendorRegistrationRequestDTO> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody VendorRegistrationDecisionDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.reject(id, authentication.getName(), dto));
    }
}