package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.PaymentDTO;
import com.abc.SpringBootSecqurityEx.entity.order.Order;
import com.abc.SpringBootSecqurityEx.entity.payment.Payment;
import com.abc.SpringBootSecqurityEx.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {
    private final PaymentRepository repository;
    private final EntityManager entityManager;

    public PaymentService(PaymentRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public PaymentDTO save(PaymentDTO dto) {
        Payment entity = dto.getId() == null ? new Payment() : repository.findById(dto.getId()).orElseGet(Payment::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<PaymentDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<PaymentDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(PaymentDTO dto, Payment entity) {
        entity.setId(dto.getId());
        entity.setOrder(dto.getOrderId() == null ? null : entityManager.getReference(Order.class, dto.getOrderId()));
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setPaymentGateway(dto.getPaymentGateway());
        entity.setAmount(dto.getAmount());
        entity.setStatus(dto.getStatus());
        entity.setTransactionId(dto.getTransactionId());
        entity.setPaidAt(dto.getPaidAt());
    }

    private PaymentDTO toDto(Payment entity) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrder() == null ? null : entity.getOrder().getId());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setPaymentGateway(entity.getPaymentGateway());
        dto.setAmount(entity.getAmount());
        dto.setStatus(entity.getStatus());
        dto.setTransactionId(entity.getTransactionId());
        dto.setPaidAt(entity.getPaidAt());
        return dto;
    }
}
