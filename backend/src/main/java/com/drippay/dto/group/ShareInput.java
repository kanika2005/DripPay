package com.drippay.dto.group;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ShareInput(
        @NotNull Long userId,
        @NotNull BigDecimal amount
) {
}
