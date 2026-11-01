package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.VendorOrderItemDTO;
import com.abc.SpringBootSecqurityEx.entity.order.OrderItem;
import com.abc.SpringBootSecqurityEx.entity.order.VendorOrder;
import com.abc.SpringBootSecqurityEx.entity.order.VendorOrderItem;
import com.abc.SpringBootSecqurityEx.repository.VendorOrderItemRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VendorOrderItemService {
    private final VendorOrderItemRepository repository;
    private final EntityManager entityManager;

    public VendorOrderItemService(VendorOrderItemRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public VendorOrderItemDTO save(VendorOrderItemDTO dto) {
        VendorOrderItem entity = dto.getId() == null ? new VendorOrderItem() : repository.findById(dto.getId()).orElseGet(VendorOrderItem::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<VendorOrderItemDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<VendorOrderItemDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(VendorOrderItemDTO dto, VendorOrderItem entity) {
        entity.setId(dto.getId());
        entity.setVendorOrder(dto.getVendorOrderId() == null ? null : entityManager.getReference(VendorOrder.class, dto.getVendorOrderId()));
        entity.setOrderItem(dto.getOrderItemId() == null ? null : entityManager.getReference(OrderItem.class, dto.getOrderItemId()));
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setTotalPrice(dto.getTotalPrice());
    }

    private VendorOrderItemDTO toDto(VendorOrderItem entity) {
        VendorOrderItemDTO dto = new VendorOrderItemDTO();
        dto.setId(entity.getId());
        dto.setVendorOrderId(entity.getVendorOrder() == null ? null : entity.getVendorOrder().getId());
        dto.setOrderItemId(entity.getOrderItem() == null ? null : entity.getOrderItem().getId());
        dto.setQuantity(entity.getQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setTotalPrice(entity.getTotalPrice());
        return dto;
    }
}
