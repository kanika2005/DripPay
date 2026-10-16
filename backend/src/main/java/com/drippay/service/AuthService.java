package com.drippay.service;

import com.drippay.domain.entity.User;
import com.drippay.domain.entity.Wallet;
import com.drippay.domain.enums.UserRole;
import com.drippay.dto.auth.AuthResponse;
import com.drippay.dto.auth.LoginRequest;
import com.drippay.dto.auth.RegisterRequest;
import com.drippay.dto.auth.UserProfileResponse;
import com.drippay.exception.ApiException;
import com.drippay.repository.UserRepository;
import com.drippay.repository.WalletRepository;
import com.drippay.security.JwtService;
import com.drippay.security.UserPrincipal;
import com.drippay.util.WalletIdGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user = userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setPublicWalletId(generateUniqueWalletId());
        wallet.setBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);
        return AuthResponse.of(token, toProfile(user, wallet.getPublicWalletId()));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Wallet missing"));

        UserPrincipal principal = new UserPrincipal(user);
        return AuthResponse.of(jwtService.generateToken(principal), toProfile(user, wallet.getPublicWalletId()));
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        String publicId = walletRepository.findByUserId(userId)
                .map(Wallet::getPublicWalletId)
                .orElse(null);
        return toProfile(user, publicId);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, String fullName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        user.setFullName(fullName);
        String publicId = walletRepository.findByUserId(userId).map(Wallet::getPublicWalletId).orElse(null);
        return toProfile(user, publicId);
    }

    private String generateUniqueWalletId() {
        for (int i = 0; i < 10; i++) {
            String id = WalletIdGenerator.generate();
            if (walletRepository.findByPublicWalletId(id).isEmpty()) {
                return id;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate wallet id");
    }

    private UserProfileResponse toProfile(User user, String publicWalletId) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), publicWalletId);
    }
}
