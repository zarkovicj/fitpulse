package com.fitpulse.backend.account;

import com.fitpulse.backend.account.dto.AccountRequests;
import com.fitpulse.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountController {

    private final AccountDeletionService accountDeletionService;
    private final PasswordResetService passwordResetService;

    public AccountController(AccountDeletionService accountDeletionService, PasswordResetService passwordResetService) {
        this.accountDeletionService = accountDeletionService;
        this.passwordResetService = passwordResetService;
    }

    @DeleteMapping("/api/user/me")
    public ResponseEntity<Void> deleteOwnAccount(@Valid @RequestBody AccountRequests.DeleteAccount request,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        accountDeletionService.deleteOwn(request.password(), principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/auth/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody AccountRequests.ForgotPassword request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/auth/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody AccountRequests.ResetPassword request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
