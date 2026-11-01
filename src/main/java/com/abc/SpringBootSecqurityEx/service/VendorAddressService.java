package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.VendorAddressDTO;
import com.abc.SpringBootSecqurityEx.entity.vendor.Vendor;
import com.abc.SpringBootSecqurityEx.entity.vendor.VendorAddress;
import com.abc.SpringBootSecqurityEx.repository.VendorAddressRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VendorAddressService {
    private final VendorAddressRepository repository;
    private final EntityManager entityManager;

    public VendorAddressService(VendorAddressRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public VendorAddressDTO save(VendorAddressDTO dto) {
        VendorAddress entity = dto.getId() == null ? new VendorAddress() : repository.findById(dto.getId()).orElseGet(VendorAddress::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<VendorAddressDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<VendorAddressDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(VendorAddressDTO dto, VendorAddress entity) {
        entity.setId(dto.getId());
        entity.setVendor(dto.getVendorId() == null ? null : entityManager.getReference(Vendor.class, dto.getVendorId()));
        entity.setAddressLine1(dto.getAddressLine1());
        entity.setAddressLine2(dto.getAddressLine2());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setCountry(dto.getCountry());
        entity.setPostalCode(dto.getPostalCode());
        entity.setDefaultAddress(dto.getDefaultAddress());
    }

    private VendorAddressDTO toDto(VendorAddress entity) {
        VendorAddressDTO dto = new VendorAddressDTO();
        dto.setId(entity.getId());
        dto.setVendorId(entity.getVendor() == null ? null : entity.getVendor().getId());
        dto.setAddressLine1(entity.getAddressLine1());
        dto.setAddressLine2(entity.getAddressLine2());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setCountry(entity.getCountry());
        dto.setPostalCode(entity.getPostalCode());
        dto.setDefaultAddress(entity.getDefaultAddress());
        return dto;
    }
}
