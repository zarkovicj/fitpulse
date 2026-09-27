package com.fitpulse.backend.security;

import com.fitpulse.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenProvider {

    private static final String PASSWORD_VERSION = "pwv";

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.expirationMs());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                // promena lozinke menja vrednost i time ponistava sve ranije tokene
                .claim(PASSWORD_VERSION, passwordVersion(user))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // prazno ako token nije validan
    public Optional<Claims> tryParse(String token) {
        try {
            return Optional.of(parseClaims(token));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    // token vazi dok se lozinka ne promeni
    public static boolean matches(Claims claims, User user) {
        Long userId = claims.get("userId", Long.class);
        Long version = claims.get(PASSWORD_VERSION, Long.class);
        return user.getId().equals(userId)
                && (version != null ? version : 0L) == passwordVersion(user);
    }

    public static long passwordVersion(User user) {
        Instant changedAt = user.getPasswordChangedAt();
        return changedAt != null ? changedAt.toEpochMilli() : 0L;
    }
}
