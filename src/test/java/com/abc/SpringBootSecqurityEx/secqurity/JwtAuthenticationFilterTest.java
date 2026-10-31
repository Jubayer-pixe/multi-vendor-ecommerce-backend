package com.abc.SpringBootSecqurityEx.secqurity;

import com.abc.SpringBootSecqurityEx.controller.VendorController;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtAuthenticationFilterTest {

    @Test
    void jwtFilterIsRegisteredOnlyInsideSpringSecurityChain() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(null, null);
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new SecurityConfig().jwtAuthenticationFilterRegistration(filter);

        assertFalse(registration.isEnabled());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bearerTokenLoadsPrincipalAndAuthoritiesBeforeControllerAuthorization() throws Exception {
        JwtUtil jwtUtil = new JwtUtil() {
            @Override
            public boolean validateToken(String token) {
                return "test-token".equals(token);
            }

            @Override
            public String extractUsername(String token) {
                return "admin";
            }
        };
        UserDetails admin = User.withUsername("admin")
                .password("not-used")
                .roles("ADMIN")
                .build();
        CustomUserDetailsService userDetailsService = new CustomUserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) {
                assertEquals("admin", username);
                return admin;
            }
        };
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("admin", SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals("ROLE_ADMIN", SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void vendorProfileCreationKeepsAdminOnlyRoleCheck() throws Exception {
        PreAuthorize authorization = VendorController.class
                .getMethod("create", com.abc.SpringBootSecqurityEx.dtos.VendorDTO.class)
                .getAnnotation(PreAuthorize.class);

        assertEquals("hasRole('ADMIN')", authorization.value());
    }
}
