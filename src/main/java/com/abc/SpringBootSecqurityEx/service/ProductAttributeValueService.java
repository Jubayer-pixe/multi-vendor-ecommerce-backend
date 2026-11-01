package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.ProductAttributeValueDTO;
import com.abc.SpringBootSecqurityEx.entity.product.ProductAttribute;
import com.abc.SpringBootSecqurityEx.entity.product.ProductAttributeValue;
import com.abc.SpringBootSecqurityEx.entity.product.ProductVariant;
import com.abc.SpringBootSecqurityEx.repository.ProductAttributeValueRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductAttributeValueService {
    private final ProductAttributeValueRepository repository;
    private final EntityManager entityManager;

    public ProductAttributeValueService(ProductAttributeValueRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public ProductAttributeValueDTO save(ProductAttributeValueDTO dto) {
        ProductAttributeValue entity = dto.getId() == null ? new ProductAttributeValue() : repository.findById(dto.getId()).orElseGet(ProductAttributeValue::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<ProductAttributeValueDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ProductAttributeValueDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(ProductAttributeValueDTO dto, ProductAttributeValue entity) {
        entity.setId(dto.getId());
        entity.setProductVariant(dto.getProductVariantId() == null ? null : entityManager.getReference(ProductVariant.class, dto.getProductVariantId()));
        entity.setAttribute(dto.getAttributeId() == null ? null : entityManager.getReference(ProductAttribute.class, dto.getAttributeId()));
        entity.setValue(dto.getValue());
    }

    private ProductAttributeValueDTO toDto(ProductAttributeValue entity) {
        ProductAttributeValueDTO dto = new ProductAttributeValueDTO();
        dto.setId(entity.getId());
        dto.setProductVariantId(entity.getProductVariant() == null ? null : entity.getProductVariant().getId());
        dto.setAttributeId(entity.getAttribute() == null ? null : entity.getAttribute().getId());
        dto.setValue(entity.getValue());
        return dto;
    }
}
