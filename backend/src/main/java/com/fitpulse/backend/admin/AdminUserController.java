package com.fitpulse.backend.admin;

import com.fitpulse.backend.admin.dto.AdminUserResponse;
import com.fitpulse.backend.admin.dto.UserStatusRequest;
import com.fitpulse.backend.common.PageResponse;
import com.fitpulse.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// dostupno samo korisnicima sa rolom ADMIN
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public PageResponse<AdminUserResponse> search(@RequestParam(required = false) String search,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return adminUserService.search(search, page, size);
    }

    @PutMapping("/{id}/status")
    public AdminUserResponse setStatus(@PathVariable Long id,
                                       @Valid @RequestBody UserStatusRequest request,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return adminUserService.setActive(id, request.active(), principal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        adminUserService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}
