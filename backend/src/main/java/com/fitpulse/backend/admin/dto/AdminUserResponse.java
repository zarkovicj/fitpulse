package com.fitpulse.backend.admin.dto;

import com.fitpulse.backend.user.Role;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean active,
        Instant createdAt,
        long workoutCount
) {
}
