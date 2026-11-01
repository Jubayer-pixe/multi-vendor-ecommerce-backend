package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    long countByStatusIgnoreCase(String status);
}
