package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.WishlistDTO;
import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import com.abc.SpringBootSecqurityEx.entity.customer.Wishlist;
import com.abc.SpringBootSecqurityEx.entity.customer.WishlistItem;
import com.abc.SpringBootSecqurityEx.repository.WishlistRepository;
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
public class WishlistService {
    private final WishlistRepository repository;
    private final EntityManager entityManager;

    public WishlistService(WishlistRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public WishlistDTO save(WishlistDTO dto) {
        Wishlist entity = dto.getId() == null ? new Wishlist() : repository.findById(dto.getId()).orElseGet(Wishlist::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<WishlistDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<WishlistDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(WishlistDTO dto, Wishlist entity) {
        entity.setId(dto.getId());
        entity.setCustomer(dto.getCustomerId() == null ? null : entityManager.getReference(Customer.class, dto.getCustomerId()));
        entity.setName(dto.getName());
        entity.setCreatedAt(dto.getCreatedAt());
        preserveItems(dto.getItemIds(), entity);
    }

    private WishlistDTO toDto(Wishlist entity) {
        WishlistDTO dto = new WishlistDTO();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomer() == null ? null : entity.getCustomer().getId());
        dto.setName(entity.getName());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setItemIds(entity.getItems() == null ? List.of() : entity.getItems().stream().map(WishlistItem::getId).toList());
        return dto;
    }

    private void preserveItems(List<Long> requestedIds, Wishlist wishlist) {
        if (requestedIds == null) return;
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate wishlist item ids are not supported");
        for (Long id : requestedIds) {
            WishlistItem item = entityManager.find(WishlistItem.class, id);
            if (item == null) throw new NotFoundException("Wishlist item not found: " + id);
            if (item.getWishlist() == null || wishlist.getId() == null || !wishlist.getId().equals(item.getWishlist().getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Wishlist items cannot be reassigned through wishlist update");
        }
        List<Long> existingIds = wishlist.getItems().stream().map(WishlistItem::getId).toList();
        if (!new HashSet<>(existingIds).equals(new HashSet<>(requestedIds)) || existingIds.size() != requestedIds.size()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacing wishlist items may orphan existing items");
    }
}
