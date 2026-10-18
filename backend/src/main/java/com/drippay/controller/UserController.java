package com.drippay.controller;

import com.drippay.dto.auth.UserProfileResponse;
import com.drippay.security.SecurityUtils;
import com.drippay.service.AuthService;
import com.drippay.service.TransferPinService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;
    private final TransferPinService transferPinService;

    public UserController(AuthService authService, TransferPinService transferPinService) {
        this.authService = authService;
        this.transferPinService = transferPinService;
    }

    @GetMapping("/me")
    public UserProfileResponse me() {
        return authService.getProfile(SecurityUtils.currentUser().getId());
    }

    @PatchMapping("/me")
    public UserProfileResponse update(@RequestBody UpdateProfileBody body) {
        return authService.updateProfile(SecurityUtils.currentUser().getId(), body.fullName());
    }

    @PutMapping("/me/transfer-pin")
    public void setTransferPin(@RequestBody TransferPinBody body) {
        transferPinService.setPin(SecurityUtils.currentUser().getId(), body.pin());
    }

    public record UpdateProfileBody(@NotBlank String fullName) {
    }

    public record TransferPinBody(@NotBlank String pin) {
    }
}
