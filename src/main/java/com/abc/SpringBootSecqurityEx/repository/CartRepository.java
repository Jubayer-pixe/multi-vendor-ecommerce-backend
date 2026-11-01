package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.shopping.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
    java.util.Optional<Cart> findFirstByCustomer_User_UserNameAndStatusOrderByIdAsc(String username, String status);
}
