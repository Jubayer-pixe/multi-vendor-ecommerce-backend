package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.CustomerDTO;
import com.abc.SpringBootSecqurityEx.entity.User;
import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import com.abc.SpringBootSecqurityEx.entity.customer.Wishlist;
import com.abc.SpringBootSecqurityEx.repository.CustomerRepository;
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
public class CustomerService {
    private final CustomerRepository repository;
    private final EntityManager entityManager;

    public CustomerService(CustomerRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public CustomerDTO save(CustomerDTO dto) {
        Customer entity = dto.getId() == null ? new Customer() : repository.findById(dto.getId()).orElseGet(Customer::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<CustomerDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CustomerDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(CustomerDTO dto, Customer entity) {
        entity.setId(dto.getId());
        entity.setUser(dto.getUserName() == null ? null : entityManager.getReference(User.class, dto.getUserName()));
        entity.setFullName(dto.getFullName());
        entity.setPhone(dto.getPhone());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setLoyaltyPoints(dto.getLoyaltyPoints());
        entity.setCreatedAt(dto.getCreatedAt());
        preserveWishlists(dto.getWishlistIds(), entity);
    }

    private CustomerDTO toDto(Customer entity) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(entity.getId());
        dto.setUserName(entity.getUser() == null ? null : entity.getUser().getUserName());
        dto.setFullName(entity.getFullName());
        dto.setPhone(entity.getPhone());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setLoyaltyPoints(entity.getLoyaltyPoints());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setWishlistIds(entity.getWishlists() == null ? List.of() : entity.getWishlists().stream().map(Wishlist::getId).toList());
        return dto;
    }

    private void preserveWishlists(List<Long> requestedIds, Customer customer) {
        if (requestedIds == null) return;
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate wishlist ids are not supported");
        }
        for (Long id : requestedIds) {
            Wishlist wishlist = entityManager.find(Wishlist.class, id);
            if (wishlist == null) throw new NotFoundException("Wishlist not found: " + id);
            if (wishlist.getCustomer() == null || customer.getId() == null || !customer.getId().equals(wishlist.getCustomer().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Wishlists cannot be reassigned through customer update");
            }
        }
        List<Long> existingIds = customer.getWishlists().stream().map(Wishlist::getId).toList();
        if (!new HashSet<>(existingIds).equals(new HashSet<>(requestedIds)) || existingIds.size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacing wishlists may orphan existing wishlists");
        }
    }
}
