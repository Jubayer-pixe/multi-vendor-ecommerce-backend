package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.StudentModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentModelRepository extends JpaRepository<StudentModel, Integer> {
}
