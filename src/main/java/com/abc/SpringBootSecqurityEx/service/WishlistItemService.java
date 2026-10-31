package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.WishlistItemDTO;
import com.abc.SpringBootSecqurityEx.entity.customer.Wishlist;
import com.abc.SpringBootSecqurityEx.entity.customer.WishlistItem;
import com.abc.SpringBootSecqurityEx.entity.product.Product;
import com.abc.SpringBootSecqurityEx.repository.WishlistItemRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WishlistItemService {
    private final WishlistItemRepository repository;
    private final EntityManager entityManager;

    public WishlistItemService(WishlistItemRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public WishlistItemDTO save(WishlistItemDTO dto) {
        WishlistItem entity = dto.getId() == null ? new WishlistItem() : repository.findById(dto.getId()).orElseGet(WishlistItem::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<WishlistItemDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<WishlistItemDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(WishlistItemDTO dto, WishlistItem entity) {
        entity.setId(dto.getId());
        entity.setWishlist(dto.getWishlistId() == null ? null : entityManager.getReference(Wishlist.class, dto.getWishlistId()));
        entity.setProduct(dto.getProductId() == null ? null : entityManager.getReference(Product.class, dto.getProductId()));
        entity.setQuantity(dto.getQuantity());
        entity.setCreatedAt(dto.getCreatedAt());
    }

    private WishlistItemDTO toDto(WishlistItem entity) {
        WishlistItemDTO dto = new WishlistItemDTO();
        dto.setId(entity.getId());
        dto.setWishlistId(entity.getWishlist() == null ? null : entity.getWishlist().getId());
        dto.setProductId(entity.getProduct() == null ? null : entity.getProduct().getId());
        dto.setQuantity(entity.getQuantity());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
