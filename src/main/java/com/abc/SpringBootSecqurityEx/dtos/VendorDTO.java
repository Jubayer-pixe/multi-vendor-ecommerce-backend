package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class VendorDTO {
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String userName;

    @NotBlank
    @Size(max = 255)
    private String businessName;

    @NotBlank
    @Size(max = 255)
    private String contactPerson;

    @Size(max = 20)
    private String phone;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    private Boolean verified;

    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal rating;
    private LocalDateTime createdAt;
    private List<Long> addressIds;
    private List<Long> bankAccountIds;
    private List<Long> documentIds;
    private Long walletId;
}
