package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.order.VendorOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorOrderRepository extends JpaRepository<VendorOrder, Long> {
}
