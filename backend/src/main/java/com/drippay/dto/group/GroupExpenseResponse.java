package com.drippay.dto.group;

import java.math.BigDecimal;
import java.time.Instant;

public record GroupExpenseResponse(Long id, String title, BigDecimal totalAmount, Instant createdAt) {
}
