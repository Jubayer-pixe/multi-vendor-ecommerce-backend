package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class VendorWalletDTO {
    private Long id;
    private Long vendorId;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime updatedAt;
}
