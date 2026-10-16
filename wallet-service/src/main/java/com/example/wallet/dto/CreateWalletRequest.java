package com.example.wallet.dto;

/**
 * Request DTO for POST /api/v1/wallets
 *
 * Why a DTO instead of using the Wallet entity?
 * ---------------------------------------------
 * 1. API shape ≠ database shape
 *    Client sends only userId. Entity has id, balance, timestamps, etc.
 * 2. Security
 *    If we accepted Wallet entity as JSON, a client could try to set balance=999999.
 * 3. Stability
 *    Entity changes (new columns) should not force API clients to change.
 *
 * Flow:
 *   Request JSON → CreateWalletRequest → Service → Wallet Entity → DB
 */
public class CreateWalletRequest {

    private Long userId;

    public CreateWalletRequest() {
    }

    public CreateWalletRequest(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
