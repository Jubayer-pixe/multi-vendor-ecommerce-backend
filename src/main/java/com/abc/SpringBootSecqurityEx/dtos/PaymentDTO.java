package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PaymentDTO {
    private Long id;
    private Long orderId;
    private String paymentMethod;
    private String paymentGateway;
    private BigDecimal amount;
    private String status;
    private String transactionId;
    private LocalDateTime paidAt;
}
