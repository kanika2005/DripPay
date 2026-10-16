package com.example.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO returned to the client for wallet operations.
 *
 * Flow:
 *   DB → Wallet Entity → WalletResponse → JSON Response
 *
 * We never return the JPA entity directly from the controller.
 */
public class WalletResponse {

    private Long id;
    private Long userId;
    private BigDecimal balance;
    private Instant createdAt;
    private Instant updatedAt;

    public WalletResponse() {
    }

    public WalletResponse(Long id, Long userId, BigDecimal balance, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
