package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.CouponDTO;
import com.abc.SpringBootSecqurityEx.entity.promotion.Coupon;
import com.abc.SpringBootSecqurityEx.repository.CouponRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CouponService {
    private final CouponRepository repository;
    private final EntityManager entityManager;

    public CouponService(CouponRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public CouponDTO save(CouponDTO dto) {
        Coupon entity = dto.getId() == null ? new Coupon() : repository.findById(dto.getId()).orElseGet(Coupon::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<CouponDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CouponDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(CouponDTO dto, Coupon entity) {
        entity.setId(dto.getId());
        entity.setCode(dto.getCode());
        entity.setDiscountType(dto.getDiscountType());
        entity.setDiscountValue(dto.getDiscountValue());
        entity.setMinOrderValue(dto.getMinOrderValue());
        entity.setMaxDiscount(dto.getMaxDiscount());
        entity.setValidFrom(dto.getValidFrom());
        entity.setValidTo(dto.getValidTo());
        entity.setActive(dto.getActive());
    }

    private CouponDTO toDto(Coupon entity) {
        CouponDTO dto = new CouponDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setDiscountType(entity.getDiscountType());
        dto.setDiscountValue(entity.getDiscountValue());
        dto.setMinOrderValue(entity.getMinOrderValue());
        dto.setMaxDiscount(entity.getMaxDiscount());
        dto.setValidFrom(entity.getValidFrom());
        dto.setValidTo(entity.getValidTo());
        dto.setActive(entity.getActive());
        return dto;
    }
}
