package com.abc.SpringBootSecqurityEx.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VendorRegistrationDecisionDTO {

    @NotBlank
    private String reason;
}