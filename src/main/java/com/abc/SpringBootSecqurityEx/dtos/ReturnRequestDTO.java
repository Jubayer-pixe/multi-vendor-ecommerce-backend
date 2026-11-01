package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReturnRequestDTO {
    private Long id;
    private Long orderId;
    private Long customerId;
    private String status;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime resolvedAt;
}
