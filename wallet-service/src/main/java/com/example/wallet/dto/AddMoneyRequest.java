package com.example.wallet.dto;

import java.math.BigDecimal;

/**
 * Request DTO for POST /api/v1/wallets/{walletId}/credit
 */
public class AddMoneyRequest {

    private BigDecimal amount;

    public AddMoneyRequest() {
    }

    public AddMoneyRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
