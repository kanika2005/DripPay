package com.drippay.dto.wallet;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(
        Long id,
        String publicWalletId,
        BigDecimal balance,
        String currency,
        Instant updatedAt
) {
}
