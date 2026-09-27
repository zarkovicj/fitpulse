package com.fitpulse.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull(message = "Status je obavezan") Boolean active) {
}
