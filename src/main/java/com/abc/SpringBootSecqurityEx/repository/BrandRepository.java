package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.product.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    java.util.Optional<Brand> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
