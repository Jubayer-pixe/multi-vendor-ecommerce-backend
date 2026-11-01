package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.ProductVariantDTO;
import com.abc.SpringBootSecqurityEx.entity.product.Product;
import com.abc.SpringBootSecqurityEx.entity.product.ProductVariant;
import com.abc.SpringBootSecqurityEx.entity.product.ProductAttributeValue;
import com.abc.SpringBootSecqurityEx.repository.ProductVariantRepository;
import com.abc.SpringBootSecqurityEx.util.NotFoundException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProductVariantService {
    private final ProductVariantRepository repository;
    private final EntityManager entityManager;

    public ProductVariantService(ProductVariantRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public ProductVariantDTO save(ProductVariantDTO dto) {
        ProductVariant entity = dto.getId() == null ? new ProductVariant() : repository.findById(dto.getId()).orElseGet(ProductVariant::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariantDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ProductVariantDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(ProductVariantDTO dto, ProductVariant entity) {
        entity.setId(dto.getId());
        entity.setProduct(dto.getProductId() == null ? null : entityManager.getReference(Product.class, dto.getProductId()));
        entity.setName(dto.getName());
        entity.setSku(dto.getSku());
        entity.setPrice(dto.getPrice());
        entity.setStockQuantity(dto.getStockQuantity());
        preserveAttributeValues(dto.getAttributeValueIds(), entity);
    }

    private ProductVariantDTO toDto(ProductVariant entity) {
        ProductVariantDTO dto = new ProductVariantDTO();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProduct() == null ? null : entity.getProduct().getId());
        dto.setName(entity.getName());
        dto.setSku(entity.getSku());
        dto.setPrice(entity.getPrice());
        dto.setStockQuantity(entity.getStockQuantity());
        dto.setAttributeValueIds(entity.getAttributeValues() == null ? List.of() : entity.getAttributeValues().stream().map(ProductAttributeValue::getId).toList());
        return dto;
    }

    private void preserveAttributeValues(List<Long> requestedIds, ProductVariant variant) {
        if (requestedIds == null) return;
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate attribute value ids are not supported");
        }
        for (Long id : requestedIds) {
            ProductAttributeValue value = entityManager.find(ProductAttributeValue.class, id);
            if (value == null) throw new NotFoundException("Product attribute value not found: " + id);
            if (value.getProductVariant() == null || variant.getId() == null || !variant.getId().equals(value.getProductVariant().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Attribute values cannot be reassigned through variant update");
            }
        }
        List<Long> existingIds = variant.getAttributeValues().stream().map(ProductAttributeValue::getId).toList();
        if (!new HashSet<>(existingIds).equals(new HashSet<>(requestedIds)) || existingIds.size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacing attribute values may orphan existing values");
        }
    }
}
