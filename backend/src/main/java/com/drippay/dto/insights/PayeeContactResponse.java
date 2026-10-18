package com.drippay.dto.insights;

import java.math.BigDecimal;
import java.time.Instant;

public record PayeeContactResponse(
        String walletId,
        String displayName,
        String dripHandle,
        Instant lastPaidAt,
        BigDecimal lastAmount,
        long transferCount
) {
}
