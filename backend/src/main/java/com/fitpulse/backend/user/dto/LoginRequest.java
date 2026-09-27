package com.fitpulse.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Mail je obavezan") String email,
        @NotBlank(message = "Lozinka je obavezna") String password
) {
}
