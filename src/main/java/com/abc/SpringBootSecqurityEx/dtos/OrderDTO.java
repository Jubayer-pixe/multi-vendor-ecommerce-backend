package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OrderDTO {
    private Long id;
    private Long customerId;
    private String orderNumber;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private List<Long> itemIds;
}
