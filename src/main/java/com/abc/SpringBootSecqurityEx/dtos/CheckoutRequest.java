package com.abc.SpringBootSecqurityEx.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CheckoutRequest {
    @NotBlank @Size(max = 255)
    private String customerName;
    @NotBlank @Size(max = 20)
    private String phone;
    @NotBlank @Size(max = 255)
    private String address;
    @NotBlank @Size(max = 100)
    private String city;
    @NotBlank @Size(max = 30)
    private String postalCode;
    @NotBlank
    private String paymentMethod;
    @Size(max = 1000)
    private String customerNote;
    @NotEmpty @Valid
    private List<CheckoutItem> items;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CheckoutItem {
        @NotNull @Positive
        private Long productId;
        @NotNull @Positive
        private Integer quantity;
    }
}
