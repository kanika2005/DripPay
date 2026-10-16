package com.example.wallet.dto;

import java.math.BigDecimal;

/**
 * Request DTO for POST /api/v1/wallets/{walletId}/debit
 */
public class WithdrawMoneyRequest {

    private BigDecimal amount;

    public WithdrawMoneyRequest() {
    }

    public WithdrawMoneyRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
