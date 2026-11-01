package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.VendorDTO;
import com.abc.SpringBootSecqurityEx.entity.User;
import com.abc.SpringBootSecqurityEx.enums.ERole;
import com.abc.SpringBootSecqurityEx.entity.vendor.Vendor;
import com.abc.SpringBootSecqurityEx.entity.vendor.VendorWallet;
import com.abc.SpringBootSecqurityEx.entity.vendor.VendorBankAccount;
import com.abc.SpringBootSecqurityEx.entity.vendor.VendorDocument;
import com.abc.SpringBootSecqurityEx.repository.VendorRepository;
import com.abc.SpringBootSecqurityEx.repository.UserRepository;
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
public class VendorService {
    private final VendorRepository repository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public VendorService(VendorRepository repository, UserRepository userRepository, EntityManager entityManager) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    public VendorDTO save(VendorDTO dto) {
        if (dto.getId() == null) {
            User user = requireVendorUser(dto.getUserName());
            if (repository.existsByUser_UserName(user.getUserName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A vendor profile already exists for this user");
            }
            if (repository.existsByEmail(dto.getEmail())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Vendor email is already in use");
            }
            Vendor entity = new Vendor();
            copyToEntity(dto, entity, user);
            return toDto(repository.save(entity));
        }

        Vendor entity = repository.findById(dto.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor profile not found"));
        User user = requireVendorUser(dto.getUserName());
        String currentUsername = entity.getUser() == null ? null : entity.getUser().getUserName();
        if (!user.getUserName().equals(currentUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vendor profile cannot be reassigned to another user");
        }
        if (repository.existsByUser_UserNameAndIdNot(user.getUserName(), entity.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A vendor profile already exists for this user");
        }
        if (repository.existsByEmailAndIdNot(dto.getEmail(), entity.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vendor email is already in use");
        }

        copyToEntity(dto, entity, user);
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<VendorDTO> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<VendorDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private User requireVendorUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userName is required");
        }
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRoles() == null || !user.getRoles().contains(ERole.VENDOR)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The selected user does not have the VENDOR role");
        }
        return user;
    }

    private void copyToEntity(VendorDTO dto, Vendor entity, User user) {
        entity.setId(dto.getId());
        entity.setUser(user);
        entity.setBusinessName(dto.getBusinessName());
        entity.setContactPerson(dto.getContactPerson());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        if (dto.getVerified() != null) entity.setVerified(dto.getVerified());
        if (dto.getRating() != null) entity.setRating(dto.getRating());
        if (dto.getCreatedAt() != null) entity.setCreatedAt(dto.getCreatedAt());
        preserveBankAccounts(dto.getBankAccountIds(), entity);
        preserveDocuments(dto.getDocumentIds(), entity);
        entity.setWallet(dto.getWalletId() == null ? null : entityManager.getReference(VendorWallet.class, dto.getWalletId()));
    }

    private VendorDTO toDto(Vendor entity) {
        VendorDTO dto = new VendorDTO();
        dto.setId(entity.getId());
        dto.setUserName(entity.getUser() == null ? null : entity.getUser().getUserName());
        dto.setBusinessName(entity.getBusinessName());
        dto.setContactPerson(entity.getContactPerson());
        dto.setPhone(entity.getPhone());
        dto.setEmail(entity.getEmail());
        dto.setVerified(entity.getVerified());
        dto.setRating(entity.getRating());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setWalletId(entity.getWallet() == null ? null : entity.getWallet().getId());
        dto.setBankAccountIds(entity.getBankAccounts() == null ? java.util.List.of() : entity.getBankAccounts().stream().map(VendorBankAccount::getId).toList());
        dto.setDocumentIds(entity.getDocuments() == null ? java.util.List.of() : entity.getDocuments().stream().map(VendorDocument::getId).toList());
        return dto;
    }

    private void preserveBankAccounts(List<Long> requestedIds, Vendor vendor) {
        if (requestedIds == null) return;
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate bank account ids are not supported");
        for (Long id : requestedIds) {
            VendorBankAccount account = entityManager.find(VendorBankAccount.class, id);
            if (account == null) throw new NotFoundException("Vendor bank account not found: " + id);
            if (account.getVendor() == null || vendor.getId() == null || !vendor.getId().equals(account.getVendor().getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Bank accounts cannot be reassigned through vendor update");
        }
        List<Long> existingIds = vendor.getBankAccounts().stream().map(VendorBankAccount::getId).toList();
        if (!new HashSet<>(existingIds).equals(new HashSet<>(requestedIds)) || existingIds.size() != requestedIds.size()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacing bank accounts may orphan existing accounts");
    }

    private void preserveDocuments(List<Long> requestedIds, Vendor vendor) {
        if (requestedIds == null) return;
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate document ids are not supported");
        for (Long id : requestedIds) {
            VendorDocument document = entityManager.find(VendorDocument.class, id);
            if (document == null) throw new NotFoundException("Vendor document not found: " + id);
            if (document.getVendor() == null || vendor.getId() == null || !vendor.getId().equals(document.getVendor().getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Documents cannot be reassigned through vendor update");
        }
        List<Long> existingIds = vendor.getDocuments().stream().map(VendorDocument::getId).toList();
        if (!new HashSet<>(existingIds).equals(new HashSet<>(requestedIds)) || existingIds.size() != requestedIds.size()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacing documents may orphan existing documents");
    }
}
