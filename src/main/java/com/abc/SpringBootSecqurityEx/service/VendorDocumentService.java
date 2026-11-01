package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.VendorDocumentDTO;
import com.abc.SpringBootSecqurityEx.entity.vendor.Vendor;
import com.abc.SpringBootSecqurityEx.entity.vendor.VendorDocument;
import com.abc.SpringBootSecqurityEx.repository.VendorDocumentRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VendorDocumentService {
    private final VendorDocumentRepository repository;
    private final EntityManager entityManager;

    public VendorDocumentService(VendorDocumentRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public VendorDocumentDTO save(VendorDocumentDTO dto) {
        VendorDocument entity = dto.getId() == null ? new VendorDocument() : repository.findById(dto.getId()).orElseGet(VendorDocument::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<VendorDocumentDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<VendorDocumentDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(VendorDocumentDTO dto, VendorDocument entity) {
        entity.setId(dto.getId());
        entity.setVendor(dto.getVendorId() == null ? null : entityManager.getReference(Vendor.class, dto.getVendorId()));
        entity.setDocumentType(dto.getDocumentType());
        entity.setDocumentNumber(dto.getDocumentNumber());
        entity.setFileUrl(dto.getFileUrl());
        entity.setStatus(dto.getStatus());
    }

    private VendorDocumentDTO toDto(VendorDocument entity) {
        VendorDocumentDTO dto = new VendorDocumentDTO();
        dto.setId(entity.getId());
        dto.setVendorId(entity.getVendor() == null ? null : entity.getVendor().getId());
        dto.setDocumentType(entity.getDocumentType());
        dto.setDocumentNumber(entity.getDocumentNumber());
        dto.setFileUrl(entity.getFileUrl());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
