package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.VendorWalletDTO;
import com.abc.SpringBootSecqurityEx.entity.vendor.Vendor;
import com.abc.SpringBootSecqurityEx.entity.vendor.VendorWallet;
import com.abc.SpringBootSecqurityEx.repository.VendorWalletRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VendorWalletService {
    private final VendorWalletRepository repository;
    private final EntityManager entityManager;

    public VendorWalletService(VendorWalletRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public VendorWalletDTO save(VendorWalletDTO dto) {
        VendorWallet entity = dto.getId() == null ? new VendorWallet() : repository.findById(dto.getId()).orElseGet(VendorWallet::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<VendorWalletDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<VendorWalletDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(VendorWalletDTO dto, VendorWallet entity) {
        entity.setId(dto.getId());
        entity.setVendor(dto.getVendorId() == null ? null : entityManager.getReference(Vendor.class, dto.getVendorId()));
        entity.setBalance(dto.getBalance());
        entity.setCurrency(dto.getCurrency());
        entity.setUpdatedAt(dto.getUpdatedAt());
    }

    private VendorWalletDTO toDto(VendorWallet entity) {
        VendorWalletDTO dto = new VendorWalletDTO();
        dto.setId(entity.getId());
        dto.setVendorId(entity.getVendor() == null ? null : entity.getVendor().getId());
        dto.setBalance(entity.getBalance());
        dto.setCurrency(entity.getCurrency());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
