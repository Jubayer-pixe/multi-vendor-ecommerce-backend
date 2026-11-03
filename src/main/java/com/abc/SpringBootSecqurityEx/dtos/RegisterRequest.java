package com.abc.SpringBootSecqurityEx.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(max = 100)
    private String username;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String lastName;

    private String accountType; // CUSTOMER or VENDOR

    // vendor fields
    @Size(max = 255)
    private String businessName;

    @Size(max = 255)
    private String contactPerson;

    @Size(max = 30)
    private String businessPhone;

    @Size(max = 500)
    private String businessAddress;

    @Size(max = 2000)
    private String businessDescription;

    @Size(max = 255)
    private String tradeLicenseNumber;
}