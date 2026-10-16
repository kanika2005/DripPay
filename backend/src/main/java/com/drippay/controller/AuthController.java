package com.drippay.controller;

import com.drippay.dto.auth.AuthResponse;
import com.drippay.dto.auth.LoginRequest;
import com.drippay.dto.auth.RegisterRequest;
import com.drippay.service.AuthService;
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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT is stateless — client discards token
        return ResponseEntity.noContent().build();
    }
}
