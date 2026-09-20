package com.fitpulse.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Ime je obavezno") String ime,
        @NotBlank(message = "Prezime je obavezno") String prezime,
        @NotBlank(message = "Mail je obavezan") @Email(message = "Mail nije validan") String mail,
        @NotBlank(message = "Lozinka je obavezna") @Size(min = 6, message = "Lozinka mora imati bar 6 karaktera") String password,
        LocalDate datumRodjenja
) {
}
