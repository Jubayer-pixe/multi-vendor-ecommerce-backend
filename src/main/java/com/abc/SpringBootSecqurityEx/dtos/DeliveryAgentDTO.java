package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class DeliveryAgentDTO {
    private Long id;
    private String userName;
    private String fullName;
    private String phone;
    private String vehicleType;
    private BigDecimal rating;
    private Boolean active;
    private LocalDateTime createdAt;
}
