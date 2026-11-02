package com.abc.SpringBootSecqurityEx.dtos;

import com.abc.SpringBootSecqurityEx.entity.delivery.DeliveryStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class DeliveryDTO {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long deliveryAgentId;
    private String deliveryAgentName;
    private DeliveryStatus status;
    private String deliveryAddress;
    private String trackingNote;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}