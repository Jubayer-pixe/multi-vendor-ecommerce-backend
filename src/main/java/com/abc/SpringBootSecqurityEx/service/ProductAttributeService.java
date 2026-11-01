package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.ProductAttributeDTO;
import com.abc.SpringBootSecqurityEx.entity.product.ProductAttribute;
import com.abc.SpringBootSecqurityEx.repository.ProductAttributeRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductAttributeService {
    private final ProductAttributeRepository repository;
    private final EntityManager entityManager;

    public ProductAttributeService(ProductAttributeRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public ProductAttributeDTO save(ProductAttributeDTO dto) {
        ProductAttribute entity = dto.getId() == null ? new ProductAttribute() : repository.findById(dto.getId()).orElseGet(ProductAttribute::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<ProductAttributeDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ProductAttributeDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(ProductAttributeDTO dto, ProductAttribute entity) {
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDisplayName(dto.getDisplayName());
    }

    private ProductAttributeDTO toDto(ProductAttribute entity) {
        ProductAttributeDTO dto = new ProductAttributeDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDisplayName(entity.getDisplayName());
        return dto;
    }
}
