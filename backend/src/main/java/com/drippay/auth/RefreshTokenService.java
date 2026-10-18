package com.drippay.auth;

import com.drippay.exception.ApiException;
import com.drippay.util.RequestHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final long refreshTtlDays;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${drippay.jwt.refresh-expiration-days:14}") long refreshTtlDays) {
        this.repository = repository;
        this.refreshTtlDays = refreshTtlDays;
    }

    @Transactional
    public String issueForUser(Long userId) {
        String raw = UUID.randomUUID().toString() + UUID.randomUUID();
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(RequestHashUtil.sha256(raw));
        token.setExpiresAt(Instant.now().plusSeconds(refreshTtlDays * 24 * 3600));
        repository.save(token);
        return raw;
    }

    @Transactional
    public RefreshToken validateAndConsume(String rawToken) {
        String hash = RequestHashUtil.sha256(rawToken);
        RefreshToken token = repository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            repository.delete(token);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        repository.delete(token);
        return token;
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = RequestHashUtil.sha256(rawToken);
        repository.findByTokenHash(hash).ifPresent(repository::delete);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.deleteByUserId(userId);
    }
}
