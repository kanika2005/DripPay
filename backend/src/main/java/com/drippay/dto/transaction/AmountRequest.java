package com.drippay.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AmountRequest(
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
        String note
) {
}
