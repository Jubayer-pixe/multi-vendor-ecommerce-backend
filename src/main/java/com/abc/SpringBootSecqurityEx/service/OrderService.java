package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.OrderDTO;
import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import com.abc.SpringBootSecqurityEx.entity.order.Order;
import com.abc.SpringBootSecqurityEx.entity.order.OrderItem;
import com.abc.SpringBootSecqurityEx.repository.OrderRepository;
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
public class OrderService {
    private final OrderRepository repository;
    private final EntityManager entityManager;

    public OrderService(OrderRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public OrderDTO save(OrderDTO dto) {
        Order entity = dto.getId() == null ? new Order() : repository.findById(dto.getId()).orElseGet(Order::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<OrderDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(OrderDTO dto, Order entity) {
        entity.setId(dto.getId());
        entity.setCustomer(dto.getCustomerId() == null ? null : entityManager.getReference(Customer.class, dto.getCustomerId()));
        entity.setOrderNumber(dto.getOrderNumber());
        entity.setStatus(dto.getStatus());
        entity.setSubtotal(dto.getSubtotal());
        entity.setShippingFee(dto.getShippingFee());
        entity.setDiscount(dto.getDiscount());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setCreatedAt(dto.getCreatedAt());
        preserveItems(dto.getItemIds(), entity);
    }

    private OrderDTO toDto(Order entity) {
        OrderDTO dto = new OrderDTO();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomer() == null ? null : entity.getCustomer().getId());
        dto.setOrderNumber(entity.getOrderNumber());
        dto.setStatus(entity.getStatus());
        dto.setSubtotal(entity.getSubtotal());
        dto.setShippingFee(entity.getShippingFee());
        dto.setDiscount(entity.getDiscount());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setItemIds(entity.getItems() == null ? List.of() : entity.getItems().stream().map(OrderItem::getId).toList());
        return dto;
    }

    private void preserveItems(List<Long> requestedIds, Order order) {
        if (requestedIds == null) return;
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate order item ids are not supported");
        }
        for (Long id : requestedIds) {
            OrderItem item = entityManager.find(OrderItem.class, id);
            if (item == null) throw new NotFoundException("Order item not found: " + id);
            if (item.getOrder() == null || order.getId() == null || !order.getId().equals(item.getOrder().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Order items cannot be reassigned through order update");
            }
        }
        List<Long> existingIds = order.getItems().stream().map(OrderItem::getId).toList();
        if (!new HashSet<>(existingIds).equals(new HashSet<>(requestedIds)) || existingIds.size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacing order items may orphan historical order items");
        }
    }
}
