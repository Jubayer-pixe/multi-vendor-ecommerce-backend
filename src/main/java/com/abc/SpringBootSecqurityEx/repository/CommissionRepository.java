package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.commission.Commission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRepository extends JpaRepository<Commission, Long> {
}
