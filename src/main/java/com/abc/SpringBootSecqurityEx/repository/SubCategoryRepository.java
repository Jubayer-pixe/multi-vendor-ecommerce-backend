package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.product.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
}
