package com.drippay.dto.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotBlank String categoryCode,
        @NotNull @DecimalMin("1.00") BigDecimal monthlyLimit,
        String monthYear
) {
}
