package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.CustomerAddressDTO;
import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import com.abc.SpringBootSecqurityEx.entity.customer.CustomerAddress;
import com.abc.SpringBootSecqurityEx.repository.CustomerAddressRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerAddressService {
    private final CustomerAddressRepository repository;
    private final EntityManager entityManager;

    public CustomerAddressService(CustomerAddressRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public CustomerAddressDTO save(CustomerAddressDTO dto) {
        CustomerAddress entity = dto.getId() == null ? new CustomerAddress() : repository.findById(dto.getId()).orElseGet(CustomerAddress::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<CustomerAddressDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CustomerAddressDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(CustomerAddressDTO dto, CustomerAddress entity) {
        entity.setId(dto.getId());
        entity.setCustomer(dto.getCustomerId() == null ? null : entityManager.getReference(Customer.class, dto.getCustomerId()));
        entity.setAddressLine1(dto.getAddressLine1());
        entity.setAddressLine2(dto.getAddressLine2());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setCountry(dto.getCountry());
        entity.setPostalCode(dto.getPostalCode());
        entity.setDefaultAddress(dto.getDefaultAddress());
    }

    private CustomerAddressDTO toDto(CustomerAddress entity) {
        CustomerAddressDTO dto = new CustomerAddressDTO();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomer() == null ? null : entity.getCustomer().getId());
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
