package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CheckoutResponse {
    private Long id;
    private String orderNumber;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private String customerName;
    private String phone;
    private String address;
    private String city;
    private String postalCode;
    private String paymentMethod;
}
