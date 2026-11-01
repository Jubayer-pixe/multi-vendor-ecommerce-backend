package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.order.VendorOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorOrderItemRepository extends JpaRepository<VendorOrderItem, Long> {
}
