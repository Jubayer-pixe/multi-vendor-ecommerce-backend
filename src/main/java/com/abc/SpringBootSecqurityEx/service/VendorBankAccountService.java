package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.VendorBankAccountDTO;
import com.abc.SpringBootSecqurityEx.entity.vendor.Vendor;
import com.abc.SpringBootSecqurityEx.entity.vendor.VendorBankAccount;
import com.abc.SpringBootSecqurityEx.repository.VendorBankAccountRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VendorBankAccountService {
    private final VendorBankAccountRepository repository;
    private final EntityManager entityManager;

    public VendorBankAccountService(VendorBankAccountRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public VendorBankAccountDTO save(VendorBankAccountDTO dto) {
        VendorBankAccount entity = dto.getId() == null ? new VendorBankAccount() : repository.findById(dto.getId()).orElseGet(VendorBankAccount::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<VendorBankAccountDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<VendorBankAccountDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(VendorBankAccountDTO dto, VendorBankAccount entity) {
        entity.setId(dto.getId());
        entity.setVendor(dto.getVendorId() == null ? null : entityManager.getReference(Vendor.class, dto.getVendorId()));
        entity.setBankName(dto.getBankName());
        entity.setAccountHolderName(dto.getAccountHolderName());
        entity.setAccountNumber(dto.getAccountNumber());
        entity.setIfscCode(dto.getIfscCode());
        entity.setPrimaryAccount(dto.getPrimaryAccount());
    }

    private VendorBankAccountDTO toDto(VendorBankAccount entity) {
        VendorBankAccountDTO dto = new VendorBankAccountDTO();
        dto.setId(entity.getId());
        dto.setVendorId(entity.getVendor() == null ? null : entity.getVendor().getId());
        dto.setBankName(entity.getBankName());
        dto.setAccountHolderName(entity.getAccountHolderName());
        dto.setAccountNumber(entity.getAccountNumber());
        dto.setIfscCode(entity.getIfscCode());
        dto.setPrimaryAccount(entity.getPrimaryAccount());
        return dto;
    }
}
