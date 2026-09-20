package com.fitpulse.backend.user;

import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.user.dto.UpdateUserRequest;
import com.fitpulse.backend.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CustomUserDetails principal) {
        return userService.getMe(principal.getId());
    }

    @PutMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal CustomUserDetails principal,
                                  @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateMe(principal.getId(), request);
    }
}
