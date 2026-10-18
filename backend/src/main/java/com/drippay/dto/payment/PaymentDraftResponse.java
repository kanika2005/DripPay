package com.drippay.dto.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentDraftResponse(
        Long id,
        String recipientHandle,
        String recipientWalletId,
        BigDecimal amount,
        String note,
        String status,
        Instant expiresAt
) {
}
