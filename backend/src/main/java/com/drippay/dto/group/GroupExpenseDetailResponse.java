package com.drippay.dto.group;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record GroupExpenseDetailResponse(
        Long id,
        String title,
        BigDecimal totalAmount,
        String paidByName,
        Instant createdAt,
        List<ExpenseShareLine> shares
) {
    public record ExpenseShareLine(String memberName, BigDecimal shareAmount) {
    }
}
