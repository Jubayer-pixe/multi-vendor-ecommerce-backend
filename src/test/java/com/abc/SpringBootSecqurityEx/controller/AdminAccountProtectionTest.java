package com.abc.SpringBootSecqurityEx.controller;

import com.abc.SpringBootSecqurityEx.dtos.RoleUpdateRequest;
import com.abc.SpringBootSecqurityEx.dtos.StatusUpdateRequest;
import com.abc.SpringBootSecqurityEx.entity.User;
import com.abc.SpringBootSecqurityEx.enums.ERole;
import com.abc.SpringBootSecqurityEx.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAccountProtectionTest {

    @Test
    void adminCannotDisableOwnAccount() throws Exception {
        User admin = user("admin", ERole.ADMIN);
        UserController controller = new UserController();
        injectRepository(controller, repositoryReturning(admin));
        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setEnabled(false);

        var response = controller.updateUserStatus("admin", request, authentication("admin"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(admin.getEnabled());
    }

    @Test
    void adminCannotDisableSuperAdminAccount() throws Exception {
        User root = user("root", ERole.SUPER_ADMIN);
        UserController controller = new UserController();
        injectRepository(controller, repositoryReturning(root));
        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setEnabled(false);

        var response = controller.updateUserStatus("root", request, authentication("admin"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(root.getEnabled());
    }

    @Test
    void adminCannotReassignOwnRolesOrModifySuperAdminRoles() throws Exception {
        User admin = user("admin", ERole.ADMIN);
        AdminController controller = new AdminController();
        injectRepository(controller, repositoryReturning(admin));
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRoles(EnumSet.of(ERole.CUSTOMER));

        var response = controller.updateUserRoles("admin", request, authentication("admin"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(EnumSet.of(ERole.ADMIN), admin.getRoles());

        User root = user("root", ERole.SUPER_ADMIN);
        injectRepository(controller, repositoryReturning(root));
        response = controller.updateUserRoles("root", request, authentication("admin"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(EnumSet.of(ERole.SUPER_ADMIN), root.getRoles());
    }

    private static User user(String username, ERole role) {
        User user = new User();
        user.setUserName(username);
        user.setEmail(username + "@example.test");
        user.setEnabled(true);
        user.setRoles(EnumSet.of(role));
        return user;
    }

    private static UsernamePasswordAuthenticationToken authentication(String username) {
        return UsernamePasswordAuthenticationToken.authenticated(username, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static UserRepository repositoryReturning(User selected) {
        return (UserRepository) Proxy.newProxyInstance(UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class}, (proxy, method, args) -> {
                    if (method.getName().equals("findByUserName")) return Optional.of(selected);
                    if (method.getName().equals("toString")) return "read-only user repository test proxy";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static void injectRepository(Object controller, UserRepository repository) throws Exception {
        Field field = controller.getClass().getDeclaredField("userRepository");
        field.setAccessible(true);
        field.set(controller, repository);
    }
}
