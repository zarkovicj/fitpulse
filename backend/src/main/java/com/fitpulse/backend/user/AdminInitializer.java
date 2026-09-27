package com.fitpulse.backend.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// pravi admin nalog iz ADMIN_MAIL / ADMIN_PASSWORD
@Slf4j
@Component
@EnableConfigurationProperties(AdminProperties.class)
public class AdminInitializer implements ApplicationRunner {

    private final AdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(AdminProperties properties,
                            UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            return;
        }
        if (userRepository.existsByEmail(User.normalizeEmail(properties.email()))) {
            log.info("Admin nalog {} već postoji, preskačem", properties.email());
            return;
        }

        userRepository.save(User.createAdmin(
                properties.firstName() != null ? properties.firstName() : "Admin",
                properties.lastName() != null ? properties.lastName() : "FitPulse",
                properties.email(),
                passwordEncoder.encode(properties.password())));
        log.info("Napravljen admin nalog {}", properties.email());
    }
}
