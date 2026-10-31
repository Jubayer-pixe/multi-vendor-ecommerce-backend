package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.InventoryDTO;
import com.abc.SpringBootSecqurityEx.entity.inventory.Inventory;
import com.abc.SpringBootSecqurityEx.entity.product.Product;
import com.abc.SpringBootSecqurityEx.repository.InventoryRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {
    private final InventoryRepository repository;
    private final EntityManager entityManager;

    public InventoryService(InventoryRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public InventoryDTO save(InventoryDTO dto) {
        Inventory entity = dto.getId() == null ? new Inventory() : repository.findById(dto.getId()).orElseGet(Inventory::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<InventoryDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(InventoryDTO dto, Inventory entity) {
        entity.setId(dto.getId());
        entity.setProduct(dto.getProductId() == null ? null : entityManager.getReference(Product.class, dto.getProductId()));
        entity.setVariantId(dto.getVariantId());
        entity.setAvailableQuantity(dto.getAvailableQuantity());
        entity.setReservedQuantity(dto.getReservedQuantity());
        entity.setReorderLevel(dto.getReorderLevel());
        entity.setUpdatedAt(dto.getUpdatedAt());
    }

    private InventoryDTO toDto(Inventory entity) {
        InventoryDTO dto = new InventoryDTO();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProduct() == null ? null : entity.getProduct().getId());
        dto.setVariantId(entity.getVariantId());
        dto.setAvailableQuantity(entity.getAvailableQuantity());
        dto.setReservedQuantity(entity.getReservedQuantity());
        dto.setReorderLevel(entity.getReorderLevel());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
