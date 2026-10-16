package com.drippay.dto.transaction;

import com.drippay.domain.enums.TransactionStatus;
import com.drippay.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        String referenceId,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        String currency,
        String recipientWalletId,
        String senderWalletId,
        String description,
        String categoryCode,
        Instant createdAt
) {
}
