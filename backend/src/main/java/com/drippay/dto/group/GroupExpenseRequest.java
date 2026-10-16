package com.drippay.dto.group;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record GroupExpenseRequest(
        @NotBlank String title,
        @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
        Long paidByUserId,
        List<ShareInput> shares
) {
}
