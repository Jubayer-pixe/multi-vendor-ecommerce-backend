package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.ReturnRequestDTO;
import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import com.abc.SpringBootSecqurityEx.entity.order.Order;
import com.abc.SpringBootSecqurityEx.entity.returnrequest.ReturnRequest;
import com.abc.SpringBootSecqurityEx.repository.ReturnRequestRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReturnRequestService {
    private final ReturnRequestRepository repository;
    private final EntityManager entityManager;

    public ReturnRequestService(ReturnRequestRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public ReturnRequestDTO save(ReturnRequestDTO dto) {
        ReturnRequest entity = dto.getId() == null ? new ReturnRequest() : repository.findById(dto.getId()).orElseGet(ReturnRequest::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<ReturnRequestDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(ReturnRequestDTO dto, ReturnRequest entity) {
        entity.setId(dto.getId());
        entity.setOrder(dto.getOrderId() == null ? null : entityManager.getReference(Order.class, dto.getOrderId()));
        entity.setCustomer(dto.getCustomerId() == null ? null : entityManager.getReference(Customer.class, dto.getCustomerId()));
        entity.setStatus(dto.getStatus());
        entity.setReason(dto.getReason());
        entity.setRequestedAt(dto.getRequestedAt());
        entity.setResolvedAt(dto.getResolvedAt());
    }

    private ReturnRequestDTO toDto(ReturnRequest entity) {
        ReturnRequestDTO dto = new ReturnRequestDTO();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrder() == null ? null : entity.getOrder().getId());
        dto.setCustomerId(entity.getCustomer() == null ? null : entity.getCustomer().getId());
        dto.setStatus(entity.getStatus());
        dto.setReason(entity.getReason());
        dto.setRequestedAt(entity.getRequestedAt());
        dto.setResolvedAt(entity.getResolvedAt());
        return dto;
    }
}
