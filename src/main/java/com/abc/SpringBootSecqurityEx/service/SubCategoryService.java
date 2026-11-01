package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.SubCategoryDTO;
import com.abc.SpringBootSecqurityEx.entity.product.Category;
import com.abc.SpringBootSecqurityEx.entity.product.SubCategory;
import com.abc.SpringBootSecqurityEx.repository.SubCategoryRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubCategoryService {
    private final SubCategoryRepository repository;
    private final EntityManager entityManager;

    public SubCategoryService(SubCategoryRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public SubCategoryDTO save(SubCategoryDTO dto) {
        SubCategory entity = dto.getId() == null ? new SubCategory() : repository.findById(dto.getId()).orElseGet(SubCategory::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<SubCategoryDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<SubCategoryDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(SubCategoryDTO dto, SubCategory entity) {
        entity.setId(dto.getId());
        entity.setCategory(dto.getCategoryId() == null ? null : entityManager.getReference(Category.class, dto.getCategoryId()));
        entity.setName(dto.getName());
        entity.setSlug(dto.getSlug());
    }

    private SubCategoryDTO toDto(SubCategory entity) {
        SubCategoryDTO dto = new SubCategoryDTO();
        dto.setId(entity.getId());
        dto.setCategoryId(entity.getCategory() == null ? null : entity.getCategory().getId());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());
        return dto;
    }
}
