package com.drippay.dto.group;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SettleRequest(
        @NotNull Long toUserId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String note
) {
}
