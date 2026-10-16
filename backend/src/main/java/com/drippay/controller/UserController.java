package com.drippay.controller;

import com.drippay.dto.auth.UserProfileResponse;
import com.drippay.security.SecurityUtils;
import com.drippay.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserProfileResponse me() {
        return authService.getProfile(SecurityUtils.currentUser().getId());
    }

    @PatchMapping("/me")
    public UserProfileResponse update(@RequestBody UpdateProfileBody body) {
        return authService.updateProfile(SecurityUtils.currentUser().getId(), body.fullName());
    }

    public record UpdateProfileBody(@NotBlank String fullName) {
    }
}
