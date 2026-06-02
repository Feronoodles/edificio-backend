package com.edificio.app.config;

import com.edificio.app.domain.AppUser;
import com.edificio.app.domain.UserRole;
import com.edificio.app.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin-username}")
    private String adminUsername;

    @Value("${app.security.admin-password}")
    private String adminPassword;

    @Value("${app.security.admin-email}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        if (appUserRepository.existsByUsername(adminUsername)) {
            return;
        }

        var admin = new AppUser();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFullName("Administrador");
        admin.setEmail(adminEmail);
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);

        appUserRepository.save(admin);
    }
}
