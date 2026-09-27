package com.fitpulse.backend.user.dto;

import com.fitpulse.backend.user.User;
import com.fitpulse.backend.user.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        LocalDate birthDate,
        BigDecimal weight,
        BigDecimal height,
        Role role,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getBirthDate(),
                user.getWeight(),
                user.getHeight(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
