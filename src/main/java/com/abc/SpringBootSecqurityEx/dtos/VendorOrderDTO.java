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
public class VendorOrderDTO {
    private Long id;
    private Long orderId;
    private Long vendorId;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal commissionAmount;
    private LocalDateTime createdAt;
    private List<Long> itemIds;
}
