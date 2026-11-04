package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.order.CheckoutIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CheckoutIdempotencyRepository extends JpaRepository<CheckoutIdempotency, Long> {
    Optional<CheckoutIdempotency> findByCustomer_IdAndIdempotencyKey(Long customerId, String idempotencyKey);
}
