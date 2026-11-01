package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class VendorOrderItemDTO {
    private Long id;
    private Long vendorOrderId;
    private Long orderItemId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
