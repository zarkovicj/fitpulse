package com.fitpulse.backend.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AccountRequests {

    private AccountRequests() {
    }

    public record DeleteAccount(@NotBlank(message = "Lozinka je obavezna") String password) {
    }

    public record ForgotPassword(
            @NotBlank(message = "Mail je obavezan") @Email(message = "Mail nije validan") String email) {

        public ForgotPassword {
            email = email != null ? email.trim() : null;
        }
    }

    public record ResetPassword(
            @NotBlank(message = "Link nije važeći") String token,
            @NotBlank(message = "Lozinka je obavezna")
            @Size(min = 6, max = 72, message = "Lozinka mora imati od 6 do 72 karaktera") String newPassword) {
    }
}
