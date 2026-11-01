package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.DeliveryAgentDTO;
import com.abc.SpringBootSecqurityEx.entity.User;
import com.abc.SpringBootSecqurityEx.entity.delivery.DeliveryAgent;
import com.abc.SpringBootSecqurityEx.repository.DeliveryAgentRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeliveryAgentService {
    private final DeliveryAgentRepository repository;
    private final EntityManager entityManager;

    public DeliveryAgentService(DeliveryAgentRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public DeliveryAgentDTO save(DeliveryAgentDTO dto) {
        DeliveryAgent entity = dto.getId() == null ? new DeliveryAgent() : repository.findById(dto.getId()).orElseGet(DeliveryAgent::new);
        copyToEntity(dto, entity);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<DeliveryAgentDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAgentDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void copyToEntity(DeliveryAgentDTO dto, DeliveryAgent entity) {
        entity.setId(dto.getId());
        entity.setUser(dto.getUserName() == null ? null : entityManager.getReference(User.class, dto.getUserName()));
        entity.setFullName(dto.getFullName());
        entity.setPhone(dto.getPhone());
        entity.setVehicleType(dto.getVehicleType());
        entity.setRating(dto.getRating());
        entity.setActive(dto.getActive());
        entity.setCreatedAt(dto.getCreatedAt());
    }

    private DeliveryAgentDTO toDto(DeliveryAgent entity) {
        DeliveryAgentDTO dto = new DeliveryAgentDTO();
        dto.setId(entity.getId());
        dto.setUserName(entity.getUser() == null ? null : entity.getUser().getUserName());
        dto.setFullName(entity.getFullName());
        dto.setPhone(entity.getPhone());
        dto.setVehicleType(entity.getVehicleType());
        dto.setRating(entity.getRating());
        dto.setActive(entity.getActive());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
