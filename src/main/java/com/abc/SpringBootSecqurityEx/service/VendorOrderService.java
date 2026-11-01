package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.VendorOrderDTO;
import com.abc.SpringBootSecqurityEx.entity.order.Order;
import com.abc.SpringBootSecqurityEx.entity.order.VendorOrder;
import com.abc.SpringBootSecqurityEx.entity.order.VendorOrderItem;
import com.abc.SpringBootSecqurityEx.entity.vendor.Vendor;
import com.abc.SpringBootSecqurityEx.repository.VendorOrderRepository;
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
public class VendorOrderService {
    private final VendorOrderRepository repository;
    private final EntityManager entityManager;

    public VendorOrderService(VendorOrderRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public VendorOrderDTO save(VendorOrderDTO dto) {
        VendorOrder entity = dto.getId() == null ? new VendorOrder() : repository.findById(dto.getId()).orElseGet(VendorOrder::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<VendorOrderDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<VendorOrderDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(VendorOrderDTO dto, VendorOrder entity) {
        entity.setId(dto.getId());
        entity.setOrder(dto.getOrderId() == null ? null : entityManager.getReference(Order.class, dto.getOrderId()));
        entity.setVendor(dto.getVendorId() == null ? null : entityManager.getReference(Vendor.class, dto.getVendorId()));
        entity.setStatus(dto.getStatus());
        entity.setSubtotal(dto.getSubtotal());
        entity.setShippingFee(dto.getShippingFee());
        entity.setCommissionAmount(dto.getCommissionAmount());
        entity.setCreatedAt(dto.getCreatedAt());
        preserveItems(dto.getItemIds(), entity);
    }

    private VendorOrderDTO toDto(VendorOrder entity) {
        VendorOrderDTO dto = new VendorOrderDTO();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrder() == null ? null : entity.getOrder().getId());
        dto.setVendorId(entity.getVendor() == null ? null : entity.getVendor().getId());
        dto.setStatus(entity.getStatus());
        dto.setSubtotal(entity.getSubtotal());
        dto.setShippingFee(entity.getShippingFee());
        dto.setCommissionAmount(entity.getCommissionAmount());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setItemIds(entity.getItems() == null ? List.of() : entity.getItems().stream().map(VendorOrderItem::getId).toList());
        return dto;
    }

    private void preserveItems(List<Long> requestedIds, VendorOrder vendorOrder) {
        if (requestedIds == null) return;
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate vendor order item ids are not supported");
        for (Long id : requestedIds) {
            VendorOrderItem item = entityManager.find(VendorOrderItem.class, id);
            if (item == null) throw new NotFoundException("Vendor order item not found: " + id);
            if (item.getVendorOrder() == null || vendorOrder.getId() == null || !vendorOrder.getId().equals(item.getVendorOrder().getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Vendor order items cannot be reassigned through vendor order update");
        }
        List<Long> existingIds = vendorOrder.getItems().stream().map(VendorOrderItem::getId).toList();
        if (!new HashSet<>(existingIds).equals(new HashSet<>(requestedIds)) || existingIds.size() != requestedIds.size()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacing vendor order items may orphan existing items");
    }
}
