package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.BrandDTO;
import com.abc.SpringBootSecqurityEx.entity.product.Brand;
import com.abc.SpringBootSecqurityEx.repository.BrandRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BrandService {
    private final BrandRepository repository;
    private final EntityManager entityManager;

    public BrandService(BrandRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public BrandDTO save(BrandDTO dto) {
        String name = dto.getName() == null ? null : dto.getName().trim();
        boolean duplicateName = dto.getId() == null ? repository.existsByNameIgnoreCase(name)
                : repository.existsByNameIgnoreCaseAndIdNot(name, dto.getId());
        if (duplicateName) throw new ResponseStatusException(HttpStatus.CONFLICT, "Brand name already exists");
        Brand entity = dto.getId() == null ? new Brand() : repository.findById(dto.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<BrandDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<BrandDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(BrandDTO dto, Brand entity) {
        entity.setId(dto.getId());
        entity.setName(dto.getName().trim());
        entity.setLogoUrl(dto.getLogoUrl());
        entity.setDescription(dto.getDescription());
    }

    private BrandDTO toDto(Brand entity) {
        BrandDTO dto = new BrandDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setLogoUrl(entity.getLogoUrl());
        dto.setDescription(entity.getDescription());
        return dto;
    }
}
