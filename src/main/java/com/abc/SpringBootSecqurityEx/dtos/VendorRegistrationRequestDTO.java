package com.abc.SpringBootSecqurityEx.dtos;

import com.abc.SpringBootSecqurityEx.enums.VendorRegistrationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class VendorRegistrationRequestDTO {

    private Long id;

    private String userName;

    @NotBlank
    @Size(max = 255)
    private String businessName;

    @NotBlank
    @Size(max = 255)
    private String contactPerson;

    @NotBlank
    @Email
    @Size(max = 255)
    private String businessEmail;

    @Size(max = 30)
    private String businessPhone;

    @Size(max = 500)
    private String businessAddress;

    @Size(max = 2000)
    private String businessDescription;

    @Size(max = 255)
    private String tradeLicenseNumber;

    private VendorRegistrationStatus status;

    private String rejectionReason;

    private String reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}