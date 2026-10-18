package com.drippay.service;

import com.drippay.domain.entity.User;
import com.drippay.domain.entity.Wallet;
import com.drippay.domain.enums.UserRole;
import com.drippay.dto.auth.AuthResponse;
import com.drippay.dto.auth.LoginRequest;
import com.drippay.dto.auth.RegisterRequest;
import com.drippay.dto.auth.UserProfileResponse;
import com.drippay.exception.ApiException;
import com.drippay.auth.RefreshTokenService;
import com.drippay.repository.UserRepository;
import com.drippay.repository.WalletRepository;
import com.drippay.security.JwtService;
import com.drippay.security.UserPrincipal;
import com.drippay.ratelimit.RateLimitService;
import com.drippay.collect.CollectService;
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
    private final RefreshTokenService refreshTokenService;
    private final RateLimitService rateLimitService;
    private final CollectService collectService;
    private final TransferPinService transferPinService;

    public AuthService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       RateLimitService rateLimitService,
                       CollectService collectService,
                       TransferPinService transferPinService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.rateLimitService = rateLimitService;
        this.collectService = collectService;
        this.transferPinService = transferPinService;
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
        collectService.ensureHandle(user);

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);
        String refresh = refreshTokenService.issueForUser(user.getId());
        return AuthResponse.of(token, refresh, toProfile(user, wallet.getPublicWalletId()));
    }

    public AuthResponse login(LoginRequest request) {
        rateLimitService.checkLogin(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Wallet missing"));

        UserPrincipal principal = new UserPrincipal(user);
        String refresh = refreshTokenService.issueForUser(user.getId());
        return AuthResponse.of(jwtService.generateToken(principal), refresh, toProfile(user, wallet.getPublicWalletId()));
    }

    public AuthResponse refresh(String rawRefreshToken) {
        var token = refreshTokenService.validateAndConsume(rawRefreshToken);
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Wallet missing"));
        UserPrincipal principal = new UserPrincipal(user);
        String access = jwtService.generateToken(principal);
        String newRefresh = refreshTokenService.issueForUser(user.getId());
        return AuthResponse.of(access, newRefresh, toProfile(user, wallet.getPublicWalletId()));
    }

    public void logout(Long userId, String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
        if (userId != null) {
            refreshTokenService.revokeAllForUser(userId);
        }
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
        String handle = user.getDripHandle();
        if (handle == null || handle.isBlank()) {
            handle = collectService.ensureHandle(user);
        }
        boolean hasPin = user.getTransferPinHash() != null && !user.getTransferPinHash().isBlank();
        return new UserProfileResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole(), publicWalletId, handle, hasPin);
    }
}
