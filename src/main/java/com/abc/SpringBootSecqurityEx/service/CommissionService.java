package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.CommissionDTO;
import com.abc.SpringBootSecqurityEx.entity.commission.Commission;
import com.abc.SpringBootSecqurityEx.entity.order.Order;
import com.abc.SpringBootSecqurityEx.entity.vendor.Vendor;
import com.abc.SpringBootSecqurityEx.repository.CommissionRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommissionService {
    private final CommissionRepository repository;
    private final EntityManager entityManager;

    public CommissionService(CommissionRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public CommissionDTO save(CommissionDTO dto) {
        Commission entity = dto.getId() == null ? new Commission() : repository.findById(dto.getId()).orElseGet(Commission::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<CommissionDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CommissionDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(CommissionDTO dto, Commission entity) {
        entity.setId(dto.getId());
        entity.setOrder(dto.getOrderId() == null ? null : entityManager.getReference(Order.class, dto.getOrderId()));
        entity.setVendor(dto.getVendorId() == null ? null : entityManager.getReference(Vendor.class, dto.getVendorId()));
        entity.setRate(dto.getRate());
        entity.setAmount(dto.getAmount());
        entity.setStatus(dto.getStatus());
        entity.setCreatedAt(dto.getCreatedAt());
    }

    private CommissionDTO toDto(Commission entity) {
        CommissionDTO dto = new CommissionDTO();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrder() == null ? null : entity.getOrder().getId());
        dto.setVendorId(entity.getVendor() == null ? null : entity.getVendor().getId());
        dto.setRate(entity.getRate());
        dto.setAmount(entity.getAmount());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
