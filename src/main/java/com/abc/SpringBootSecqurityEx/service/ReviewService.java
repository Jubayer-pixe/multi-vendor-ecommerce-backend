package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.ReviewDTO;
import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import com.abc.SpringBootSecqurityEx.entity.product.Product;
import com.abc.SpringBootSecqurityEx.entity.review.Review;
import com.abc.SpringBootSecqurityEx.repository.ReviewRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {
    private final ReviewRepository repository;
    private final EntityManager entityManager;

    public ReviewService(ReviewRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public ReviewDTO save(ReviewDTO dto) {
        Review entity = dto.getId() == null ? new Review() : repository.findById(dto.getId()).orElseGet(Review::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<ReviewDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ReviewDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(ReviewDTO dto, Review entity) {
        entity.setId(dto.getId());
        entity.setProduct(dto.getProductId() == null ? null : entityManager.getReference(Product.class, dto.getProductId()));
        entity.setCustomer(dto.getCustomerId() == null ? null : entityManager.getReference(Customer.class, dto.getCustomerId()));
        entity.setRating(dto.getRating());
        entity.setTitle(dto.getTitle());
        entity.setComment(dto.getComment());
        entity.setCreatedAt(dto.getCreatedAt());
    }

    private ReviewDTO toDto(Review entity) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProduct() == null ? null : entity.getProduct().getId());
        dto.setCustomerId(entity.getCustomer() == null ? null : entity.getCustomer().getId());
        dto.setRating(entity.getRating());
        dto.setTitle(entity.getTitle());
        dto.setComment(entity.getComment());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
