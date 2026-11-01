package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CommissionDTO {
    private Long id;
    private Long orderId;
    private Long vendorId;
    private BigDecimal rate;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
