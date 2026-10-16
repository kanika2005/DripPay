package com.drippay.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplitSmartTest {

    @Test
    void equalSplit_remainderOnFirstShare() {
        BigDecimal total = new BigDecimal("100.00");
        int members = 3;
        BigDecimal base = total.divide(BigDecimal.valueOf(members), 4, RoundingMode.DOWN);
        BigDecimal remainder = total.subtract(base.multiply(BigDecimal.valueOf(members)));

        BigDecimal first = base.add(remainder);
        BigDecimal sum = first.add(base).add(base);
        assertEquals(0, total.compareTo(sum));
    }
}
