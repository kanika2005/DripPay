package com.drippay.controller;

import com.drippay.dto.auth.AuthResponse;
import com.drippay.dto.auth.LoginRequest;
import com.drippay.dto.auth.RefreshRequest;
import com.drippay.dto.auth.RegisterRequest;
import com.drippay.service.AuthService;
import org.springframework.security.core.context.SecurityContextHolder;
import com.drippay.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;

    public AuthController(AuthService authService, AuditService auditService) {
        this.authService = authService;
        this.auditService = auditService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest http) {
        AuthResponse response = authService.register(request);
        auditService.log(response.user().id(), "REGISTER", "New account", http.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        AuthResponse response = authService.login(request);
        auditService.log(response.user().id(), "LOGIN", "Successful login", http.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        Long userId = null;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.drippay.security.UserPrincipal principal) {
            userId = principal.getId();
        }
        String refresh = request != null ? request.refreshToken() : null;
        authService.logout(userId, refresh);
        return ResponseEntity.noContent().build();
    }
}
