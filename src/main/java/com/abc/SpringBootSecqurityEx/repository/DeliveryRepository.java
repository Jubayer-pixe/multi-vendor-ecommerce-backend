package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.delivery.Delivery;
import com.abc.SpringBootSecqurityEx.entity.delivery.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByDeliveryAgent_Id(Long deliveryAgentId);

    List<Delivery> findByDeliveryAgent_IdAndStatus(
        Long deliveryAgentId,
        DeliveryStatus status
    );

    long countByDeliveryAgent_Id(Long deliveryAgentId);

    long countByDeliveryAgent_IdAndStatus(
        Long deliveryAgentId,
        DeliveryStatus status
    );

    long countByDeliveryAgent_IdAndAssignedAtGreaterThanEqualAndAssignedAtLessThan(
        Long deliveryAgentId,
        LocalDateTime start,
        LocalDateTime end
    );

    long countByDeliveryAgent_IdAndStatusAndDeliveredAtGreaterThanEqualAndDeliveredAtLessThan(
        Long deliveryAgentId,
        DeliveryStatus status,
        LocalDateTime start,
        LocalDateTime end
    );
}