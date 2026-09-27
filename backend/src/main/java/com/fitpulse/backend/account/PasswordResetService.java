package com.fitpulse.backend.account;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.user.User;
import com.fitpulse.backend.user.UserRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;


@Service
@EnableConfigurationProperties(AccountMailProperties.class)
public class PasswordResetService {

    static final Duration VALIDITY = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountMailProperties mailProperties;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                ApplicationEventPublisher eventPublisher,
                                AccountMailProperties mailProperties) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.mailProperties = mailProperties;
    }

    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(User.normalizeEmail(email))
                .filter(User::isActive)
                .ifPresent(user -> {
                    tokenRepository.deleteUnusedForUser(user.getId());
                    String rawToken = newToken();
                    tokenRepository.save(PasswordResetToken.create(user, sha256(rawToken), Instant.now().plus(VALIDITY)));
                    String link = mailProperties.frontendUrl() + "/reset-password?token=" + rawToken;
                    eventPublisher.publishEvent(new PasswordResetRequested(user.getEmail(), user.getFirstName(), link));
                });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Instant now = Instant.now();
        PasswordResetToken token = tokenRepository.findByTokenHash(sha256(rawToken))
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> ApiException.badRequest("Link za promenu lozinke nije važeći ili je istekao. Zatraži novi."));

        User user = token.getUser();

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("Nova lozinka mora da se razlikuje od stare.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(now);
        token.markUsed(now);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nije dostupan", e);
        }
    }
}
