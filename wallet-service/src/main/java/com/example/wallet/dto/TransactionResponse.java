package com.example.wallet.dto;

import com.example.wallet.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for one wallet transaction row.
 */
public class TransactionResponse {

    private Long id;
    private Long walletId;
    private TransactionType type;
    private BigDecimal amount;
    private Instant createdAt;

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, Long walletId, TransactionType type, BigDecimal amount, Instant createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
