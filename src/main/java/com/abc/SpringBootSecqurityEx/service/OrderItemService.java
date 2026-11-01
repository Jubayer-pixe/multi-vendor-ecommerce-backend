package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.OrderItemDTO;
import com.abc.SpringBootSecqurityEx.entity.order.Order;
import com.abc.SpringBootSecqurityEx.entity.order.OrderItem;
import com.abc.SpringBootSecqurityEx.entity.product.Product;
import com.abc.SpringBootSecqurityEx.repository.OrderItemRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderItemService {
    private final OrderItemRepository repository;
    private final EntityManager entityManager;

    public OrderItemService(OrderItemRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public OrderItemDTO save(OrderItemDTO dto) {
        OrderItem entity = dto.getId() == null ? new OrderItem() : repository.findById(dto.getId()).orElseGet(OrderItem::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<OrderItemDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<OrderItemDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(OrderItemDTO dto, OrderItem entity) {
        entity.setId(dto.getId());
        entity.setOrder(dto.getOrderId() == null ? null : entityManager.getReference(Order.class, dto.getOrderId()));
        entity.setProduct(dto.getProductId() == null ? null : entityManager.getReference(Product.class, dto.getProductId()));
        entity.setVariantId(dto.getVariantId());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setTotalPrice(dto.getTotalPrice());
    }

    private OrderItemDTO toDto(OrderItem entity) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrder() == null ? null : entity.getOrder().getId());
        dto.setProductId(entity.getProduct() == null ? null : entity.getProduct().getId());
        dto.setVariantId(entity.getVariantId());
        dto.setQuantity(entity.getQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setTotalPrice(entity.getTotalPrice());
        return dto;
    }
}
