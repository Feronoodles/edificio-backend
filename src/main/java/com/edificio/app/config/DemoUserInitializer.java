package com.edificio.app.config;

import com.edificio.app.domain.AppUser;
import com.edificio.app.domain.UserRole;
import com.edificio.app.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoUserInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${APP_DEMO_USERNAME:demo}")
    private String demoUsername;

    @Value("${APP_DEMO_PASSWORD:}")
    private String demoPassword;

    @Value("${APP_DEMO_EMAIL:demo@edificio.local}")
    private String demoEmail;

    @Override
    public void run(String... args) {
        if (demoPassword.isBlank()) {
            throw new IllegalStateException("APP_DEMO_PASSWORD es obligatorio cuando APP_DEMO_ENABLED=true");
        }

        var existingUser = appUserRepository.findByUsername(demoUsername);
        if (existingUser.isPresent()) {
            if (existingUser.get().getRole() != UserRole.DEMO) {
                throw new IllegalStateException("APP_DEMO_USERNAME ya pertenece a un usuario que no es DEMO");
            }
            return;
        }

        if (appUserRepository.existsByEmail(demoEmail)) {
            throw new IllegalStateException("APP_DEMO_EMAIL ya esta siendo usado por otro usuario");
        }

        var demo = new AppUser();
        demo.setUsername(demoUsername);
        demo.setPassword(passwordEncoder.encode(demoPassword));
        demo.setFullName("Usuario Demo");
        demo.setEmail(demoEmail);
        demo.setRole(UserRole.DEMO);
        demo.setActive(true);

        appUserRepository.save(demo);
    }
}
