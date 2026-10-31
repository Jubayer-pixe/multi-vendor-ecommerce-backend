
package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.entity.User;
import com.abc.SpringBootSecqurityEx.enums.ERole;
import com.abc.SpringBootSecqurityEx.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class BootstrapAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean bootstrapEnabled;

    private final String superAdminUsername;
    private final String superAdminEmail;
    private final String superAdminPassword;

    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    private final String vendorUsername;
    private final String vendorEmail;
    private final String vendorPassword;

    private final String customerUsername;
    private final String customerEmail;
    private final String customerPassword;

    private final String deliveryUsername;
    private final String deliveryEmail;
    private final String deliveryPassword;

    public BootstrapAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,

            @Value("${app.bootstrap.enabled:true}")
            boolean bootstrapEnabled,

            @Value("${app.bootstrap-super-admin.username:}")
            String superAdminUsername,
            @Value("${app.bootstrap-super-admin.email:}")
            String superAdminEmail,
            @Value("${app.bootstrap-super-admin.password:}")
            String superAdminPassword,

            @Value("${app.bootstrap-admin.username:}")
            String adminUsername,
            @Value("${app.bootstrap-admin.email:}")
            String adminEmail,
            @Value("${app.bootstrap-admin.password:}")
            String adminPassword,

            @Value("${app.bootstrap-vendor.username:}")
            String vendorUsername,
            @Value("${app.bootstrap-vendor.email:}")
            String vendorEmail,
            @Value("${app.bootstrap-vendor.password:}")
            String vendorPassword,

            @Value("${app.bootstrap-customer.username:}")
            String customerUsername,
            @Value("${app.bootstrap-customer.email:}")
            String customerEmail,
            @Value("${app.bootstrap-customer.password:}")
            String customerPassword,

            @Value("${app.bootstrap-delivery.username:}")
            String deliveryUsername,
            @Value("${app.bootstrap-delivery.email:}")
            String deliveryEmail,
            @Value("${app.bootstrap-delivery.password:}")
            String deliveryPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapEnabled = bootstrapEnabled;

        this.superAdminUsername = superAdminUsername;
        this.superAdminEmail = superAdminEmail;
        this.superAdminPassword = superAdminPassword;

        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;

        this.vendorUsername = vendorUsername;
        this.vendorEmail = vendorEmail;
        this.vendorPassword = vendorPassword;

        this.customerUsername = customerUsername;
        this.customerEmail = customerEmail;
        this.customerPassword = customerPassword;

        this.deliveryUsername = deliveryUsername;
        this.deliveryEmail = deliveryEmail;
        this.deliveryPassword = deliveryPassword;
    }

    @Override
    public void run(String... args) {

        if (!bootstrapEnabled) {
            System.out.println("Bootstrap user creation is disabled.");
            return;
        }

        createUser(
                superAdminUsername,
                superAdminEmail,
                superAdminPassword,
                "Super Admin",
                ERole.SUPER_ADMIN
        );

        createUser(
                adminUsername,
                adminEmail,
                adminPassword,
                "Administrator",
                ERole.ADMIN
        );

        createUser(
                vendorUsername,
                vendorEmail,
                vendorPassword,
                "Vendor",
                ERole.VENDOR
        );

        createUser(
                customerUsername,
                customerEmail,
                customerPassword,
                "Customer",
                ERole.CUSTOMER
        );

        createUser(
                deliveryUsername,
                deliveryEmail,
                deliveryPassword,
                "Delivery",
                ERole.DELIVERY
        );
    }

    private void createUser(
            String username,
            String email,
            String password,
            String firstName,
            ERole role
    ) {

        // Skip when this role has no bootstrap configuration.
        if (isBlank(username)
                && isBlank(email)
                && isBlank(password)) {

            System.out.println(
                    "Bootstrap configuration not provided for role: "
                            + role
            );
            return;
        }

        // Validate the configured account.
        if (isBlank(username)) {
            throw new IllegalStateException(
                    "Bootstrap username is missing for role: " + role
            );
        }

        if (isBlank(email)) {
            throw new IllegalStateException(
                    "Bootstrap email is missing for role: " + role
            );
        }

        if (isBlank(password)) {
            throw new IllegalStateException(
                    "Bootstrap password is missing for role: " + role
            );
        }

        if (password.length() < 4) {
            throw new IllegalStateException(
                    "Bootstrap password must contain at least "
                            + "4 characters for role: " + role
            );
        }

        // Do not create duplicate accounts.
        if (userRepository.existsByUserName(username)) {
            System.out.println(
                    "Bootstrap user already exists: " + username
            );
            return;
        }

        if (userRepository.existsByEmail(email)) {
            System.out.println(
                    "Bootstrap email already exists: " + email
            );
            return;
        }

        // Create a new test account.
        User user = new User();

        user.setUserName(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setUserFirstName(firstName);
        user.setRoles(EnumSet.of(role));

        userRepository.save(user);

        System.out.println("========================================");
        System.out.println("Bootstrap user created successfully");
        System.out.println("Username : " + username);
        System.out.println("Email    : " + email);
        System.out.println("Role     : " + role);
        System.out.println("========================================");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
