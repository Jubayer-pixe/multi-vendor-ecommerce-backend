package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    java.util.Optional<Customer> findByUser_UserName(String username);
}
