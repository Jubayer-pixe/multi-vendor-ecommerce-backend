package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ProductCatalogRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByActiveTrueOrderByNameAsc();
    Optional<Product> findByIdAndActiveTrue(Long id);
    long countByActiveTrue();

    long countByActiveFalse();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySkuAndIdNot(String sku, Long id);

    List<Product> findAllByVendor_User_UserNameOrderByNameAsc(String username);

    Optional<Product> findByIdAndVendor_User_UserName(Long id, String username);
}
