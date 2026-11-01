package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.delivery.DeliveryAgent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, Long> {
}
