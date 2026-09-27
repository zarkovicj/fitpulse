package com.fitpulse.backend.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    private BigDecimal weight;

    private BigDecimal height;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    // tokeni izdati pre ovog trenutka vise ne vaze
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    // blokiran nalog ne moze da se prijavi
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static User register(String firstName, String lastName, String email, String passwordHash, LocalDate birthDate) {
        User user = new User();
        user.firstName = firstName;
        user.lastName = lastName;
        user.email = normalizeEmail(email);
        user.passwordHash = passwordHash;
        user.birthDate = birthDate;
        user.role = Role.USER;
        return user;
    }

    public static User createAdmin(String firstName, String lastName, String email, String passwordHash) {
        User user = new User();
        user.firstName = firstName;
        user.lastName = lastName;
        user.email = normalizeEmail(email);
        user.passwordHash = passwordHash;
        user.role = Role.ADMIN;
        return user;
    }
}
