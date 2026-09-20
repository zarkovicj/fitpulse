package com.fitpulse.backend.user.dto;

public record AuthResponse(
        String token,
        Long userId,
        String mail,
        String role
) {
}
