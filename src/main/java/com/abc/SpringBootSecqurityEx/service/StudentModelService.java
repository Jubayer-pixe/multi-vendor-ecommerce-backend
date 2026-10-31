package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.StudentModelDTO;
import com.abc.SpringBootSecqurityEx.entity.StudentModel;
import com.abc.SpringBootSecqurityEx.repository.StudentModelRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentModelService {
    private final StudentModelRepository repository;
    private final EntityManager entityManager;

    public StudentModelService(StudentModelRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public StudentModelDTO save(StudentModelDTO dto) {
        StudentModel entity = dto.getStudentId() == null ? new StudentModel() : repository.findById(dto.getStudentId()).orElseGet(StudentModel::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<StudentModelDTO> findById(Integer id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<StudentModelDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    private void copyToEntity(StudentModelDTO dto, StudentModel entity) {
        entity.setStudentId(dto.getStudentId());
        entity.setStudentName(dto.getStudentName());
        entity.setAddress(dto.getAddress());
        entity.setSemisterCost(dto.getSemisterCost());
        entity.setSemisterNo(dto.getSemisterNo());
    }

    private StudentModelDTO toDto(StudentModel entity) {
        StudentModelDTO dto = new StudentModelDTO();
        dto.setStudentId(entity.getStudentId());
        dto.setStudentName(entity.getStudentName());
        dto.setAddress(entity.getAddress());
        dto.setSemisterCost(entity.getSemisterCost());
        dto.setSemisterNo(entity.getSemisterNo());
        return dto;
    }
}
