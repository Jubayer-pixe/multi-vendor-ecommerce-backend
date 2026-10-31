package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.CartDTO;
import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import com.abc.SpringBootSecqurityEx.entity.shopping.CartItem;
import com.abc.SpringBootSecqurityEx.entity.shopping.Cart;
import com.abc.SpringBootSecqurityEx.repository.CartRepository;
import com.abc.SpringBootSecqurityEx.util.NotFoundException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CartService {
    private final CartRepository repository;
    private final EntityManager entityManager;

    public CartService(CartRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public CartDTO save(CartDTO dto) {
        Cart entity = dto.getId() == null ? new Cart() : repository.findById(dto.getId()).orElseGet(Cart::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<CartDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CartDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(CartDTO dto, Cart entity) {
        entity.setId(dto.getId());
        entity.setCustomer(dto.getCustomerId() == null ? null : entityManager.getReference(Customer.class, dto.getCustomerId()));
        entity.setStatus(dto.getStatus());
        entity.setUpdatedAt(dto.getUpdatedAt());
        preserveItems(dto.getItemIds(), entity);
    }

    private CartDTO toDto(Cart entity) {
        CartDTO dto = new CartDTO();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomer() == null ? null : entity.getCustomer().getId());
        dto.setStatus(entity.getStatus());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setItemIds(entity.getItems() == null ? List.of() : entity.getItems().stream().map(CartItem::getId).toList());
        return dto;
    }

    private void preserveItems(List<Long> requestedIds, Cart cart) {
        if (requestedIds == null) return;
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate cart item ids are not supported");
        }
        for (Long id : requestedIds) {
            CartItem item = entityManager.find(CartItem.class, id);
            if (item == null) throw new NotFoundException("Cart item not found: " + id);
            if (item.getCart() == null || cart.getId() == null || !cart.getId().equals(item.getCart().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cart items cannot be reassigned through cart update");
            }
        }
        List<Long> existingIds = cart.getItems().stream().map(CartItem::getId).toList();
        if (!new HashSet<>(existingIds).equals(new HashSet<>(requestedIds)) || existingIds.size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacing cart items may orphan existing items");
        }
    }
}
