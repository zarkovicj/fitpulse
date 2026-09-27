package com.fitpulse.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Ime je obavezno") @Size(max = 100, message = "Ime može imati najviše 100 karaktera") String firstName,
        @NotBlank(message = "Prezime je obavezno") @Size(max = 100, message = "Prezime može imati najviše 100 karaktera") String lastName,
        @NotBlank(message = "Mail je obavezan") @Email(message = "Mail nije validan")
        @Size(max = 255, message = "Mail je predugačak") String email,
        @NotBlank(message = "Lozinka je obavezna")
        @Size(min = 6, max = 72, message = "Lozinka mora imati od 6 do 72 karaktera") String password,
        LocalDate birthDate
) {
    // razmak pre/posle maila se uklanja
    public RegisterRequest {
        email = email != null ? email.trim() : null;
    }
}
