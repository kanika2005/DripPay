package com.drippay.dto.budget;

import java.math.BigDecimal;

public record BudgetResponse(
        Long id,
        String categoryCode,
        String monthYear,
        BigDecimal monthlyLimit
) {
}
