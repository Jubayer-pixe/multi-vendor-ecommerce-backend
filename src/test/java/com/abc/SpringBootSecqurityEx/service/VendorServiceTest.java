package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.VendorDTO;
import com.abc.SpringBootSecqurityEx.entity.User;
import com.abc.SpringBootSecqurityEx.entity.vendor.Vendor;
import com.abc.SpringBootSecqurityEx.enums.ERole;
import com.abc.SpringBootSecqurityEx.repository.UserRepository;
import com.abc.SpringBootSecqurityEx.repository.VendorRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VendorServiceTest {
    private final Map<String, User> users = new HashMap<>();
    private final Map<Long, Vendor> vendors = new HashMap<>();
    private VendorRepository vendorRepository;
    private UserRepository userRepository;
    private VendorService service;

    @BeforeEach
    void setUp() {
        users.clear();
        vendors.clear();
        userRepository = repositoryProxy(UserRepository.class, (method, args) -> switch (method) {
            case "findByUserName" -> Optional.ofNullable(users.get(args[0]));
            case "existsByUserName", "existsByUserNameIgnoreCase" -> users.containsKey(args[0]);
            default -> throw new UnsupportedOperationException(method);
        });
        vendorRepository = repositoryProxy(VendorRepository.class, (method, args) -> switch (method) {
            case "findById" -> Optional.ofNullable(vendors.get(args[0]));
            case "findByUser_UserName" -> vendors.values().stream()
                    .filter(vendor -> vendor.getUser() != null && vendor.getUser().getUserName().equals(args[0]))
                    .findFirst();
            case "existsByUser_UserName" -> vendors.values().stream()
                    .anyMatch(vendor -> vendor.getUser() != null && vendor.getUser().getUserName().equals(args[0]));
            case "existsByUser_UserNameAndIdNot" -> vendors.values().stream()
                    .anyMatch(vendor -> !vendor.getId().equals(args[1]) && vendor.getUser() != null
                            && vendor.getUser().getUserName().equals(args[0]));
            case "existsByEmail" -> vendors.values().stream().anyMatch(vendor -> vendor.getEmail().equals(args[0]));
            case "existsByEmailAndIdNot" -> vendors.values().stream()
                    .anyMatch(vendor -> !vendor.getId().equals(args[1]) && vendor.getEmail().equals(args[0]));
            case "save" -> {
                Vendor vendor = (Vendor) args[0];
                if (vendor.getId() == null) vendor.setId((long) vendors.size() + 1);
                vendors.put(vendor.getId(), vendor);
                yield vendor;
            }
            case "findAll" -> new ArrayList<>(vendors.values());
            default -> throw new UnsupportedOperationException(method);
        });
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(), new Class<?>[]{EntityManager.class}, (proxy, method, args) -> {
                    if (method.getName().equals("find") || method.getName().equals("getReference")) return null;
                    if (method.getName().equals("toString")) return "EntityManager test proxy";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    throw new UnsupportedOperationException(method.toString());
                });
        service = new VendorService(vendorRepository, userRepository, entityManager);
    }

    @Test
    void createsProfileAgainstExactExistingVendorUsername() {
        User user = vendorUser("vandor");
        users.put(user.getUserName(), user);

        VendorDTO saved = service.save(vendorDto("vandor"));

        assertEquals("vandor", saved.getUserName());
        assertEquals("Store contact", saved.getContactPerson());
        assertEquals(Boolean.FALSE, saved.getVerified());
        assertEquals(BigDecimal.ZERO, saved.getRating());
        assertEquals(user, vendors.get(saved.getId()).getUser());
    }

    @Test
    void rejectsProfileCreationWhenUserDoesNotExist() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.save(vendorDto("vandor")));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(vendors.isEmpty());
    }

    @Test
    void rejectsProfileCreationWhenUserDoesNotHaveVendorRole() {
        User user = vendorUser("vandor");
        user.setRoles(EnumSet.of(ERole.CUSTOMER));
        users.put(user.getUserName(), user);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.save(vendorDto("vandor")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(vendors.isEmpty());
    }

    @Test
    void rejectsDuplicateProfileAndDuplicateVendorEmail() {
        User user = vendorUser("vandor");
        users.put(user.getUserName(), user);
        Vendor existing = new Vendor();
        existing.setId(9L);
        existing.setUser(user);
        existing.setEmail("store@example.test");
        vendors.put(existing.getId(), existing);

        ResponseStatusException duplicateUser = assertThrows(ResponseStatusException.class,
                () -> service.save(vendorDto("vandor")));
        assertEquals(HttpStatus.CONFLICT, duplicateUser.getStatusCode());

        vendors.clear();
        Vendor sameEmail = new Vendor();
        sameEmail.setId(10L);
        sameEmail.setEmail("store@example.test");
        vendors.put(sameEmail.getId(), sameEmail);
        ResponseStatusException duplicateEmail = assertThrows(ResponseStatusException.class,
                () -> service.save(vendorDto("vandor")));
        assertEquals(HttpStatus.CONFLICT, duplicateEmail.getStatusCode());
        assertEquals(1, vendors.size());
    }

    @Test
    void doesNotAllowChangingTheUserLinkedToAnExistingVendorProfile() {
        User existingUser = vendorUser("vandor");
        User otherUser = vendorUser("other-vendor");
        users.put(existingUser.getUserName(), existingUser);
        users.put(otherUser.getUserName(), otherUser);
        Vendor existing = new Vendor();
        existing.setId(4L);
        existing.setUser(existingUser);
        existing.setEmail("store@example.test");
        vendors.put(existing.getId(), existing);
        VendorDTO request = vendorDto("other-vendor");
        request.setId(existing.getId());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.save(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(existingUser, existing.getUser());
    }

    private <R> R repositoryProxy(Class<R> repositoryType, RepositoryCall call) {
        return repositoryType.cast(Proxy.newProxyInstance(repositoryType.getClassLoader(),
                new Class<?>[]{repositoryType}, (proxy, method, args) -> {
                    if (method.getName().equals("toString")) return repositoryType.getSimpleName() + " test proxy";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    return call.invoke(method.getName(), args);
                }));
    }

    private User vendorUser(String username) {
        User user = new User();
        user.setUserName(username);
        user.setEmail(username + "@example.test");
        user.setRoles(EnumSet.of(ERole.VENDOR));
        return user;
    }

    private VendorDTO vendorDto(String username) {
        VendorDTO dto = new VendorDTO();
        dto.setUserName(username);
        dto.setBusinessName("Example Store");
        dto.setContactPerson("Store contact");
        dto.setEmail("store@example.test");
        return dto;
    }

    @FunctionalInterface
    private interface RepositoryCall {
        Object invoke(String method, Object[] args);
    }
}
