package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.CartItemDTO;
import com.abc.SpringBootSecqurityEx.entity.product.Product;
import com.abc.SpringBootSecqurityEx.entity.shopping.Cart;
import com.abc.SpringBootSecqurityEx.entity.shopping.CartItem;
import com.abc.SpringBootSecqurityEx.repository.CartItemRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartItemService {
    private final CartItemRepository repository;
    private final EntityManager entityManager;

    public CartItemService(CartItemRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public CartItemDTO save(CartItemDTO dto) {
        CartItem entity = dto.getId() == null ? new CartItem() : repository.findById(dto.getId()).orElseGet(CartItem::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<CartItemDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CartItemDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(CartItemDTO dto, CartItem entity) {
        entity.setId(dto.getId());
        entity.setCart(dto.getCartId() == null ? null : entityManager.getReference(Cart.class, dto.getCartId()));
        entity.setProduct(dto.getProductId() == null ? null : entityManager.getReference(Product.class, dto.getProductId()));
        entity.setVariantId(dto.getVariantId());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setAddedAt(dto.getAddedAt());
    }

    private CartItemDTO toDto(CartItem entity) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(entity.getId());
        dto.setCartId(entity.getCart() == null ? null : entity.getCart().getId());
        dto.setProductId(entity.getProduct() == null ? null : entity.getProduct().getId());
        dto.setVariantId(entity.getVariantId());
        dto.setQuantity(entity.getQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setAddedAt(entity.getAddedAt());
        return dto;
    }
}
