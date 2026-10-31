package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.CategoryDTO;
import com.abc.SpringBootSecqurityEx.entity.product.Category;
import com.abc.SpringBootSecqurityEx.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CategoryService {
    private final CategoryRepository repository;
    private final EntityManager entityManager;

    public CategoryService(CategoryRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public CategoryDTO save(CategoryDTO dto) {
        String name = dto.getName() == null ? null : dto.getName().trim();
        String slug = dto.getSlug() == null ? null : dto.getSlug().trim();
        Long id = dto.getId();
        boolean duplicateName = id == null ? repository.existsByNameIgnoreCase(name)
                : repository.existsByNameIgnoreCaseAndIdNot(name, id);
        boolean duplicateSlug = id == null ? repository.existsBySlugIgnoreCase(slug)
                : repository.existsBySlugIgnoreCaseAndIdNot(slug, id);
        if (duplicateName) throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        if (duplicateSlug) throw new ResponseStatusException(HttpStatus.CONFLICT, "Category slug already exists");
        Category entity = id == null ? new Category() : repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<CategoryDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(CategoryDTO dto, Category entity) {
        entity.setId(dto.getId());
        entity.setName(dto.getName().trim());
        entity.setSlug(dto.getSlug().trim());
        entity.setImageUrl(dto.getImageUrl());
        if (dto.getActive() != null) entity.setActive(dto.getActive());
    }

    private CategoryDTO toDto(Category entity) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());
        dto.setImageUrl(entity.getImageUrl());
        dto.setActive(entity.getActive());
        return dto;
    }
}
