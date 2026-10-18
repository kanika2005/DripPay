package com.drippay.util;

import com.drippay.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    public static final int SCALE = 4;

    private MoneyUtils() {
    }

    public static void requireAtMostTwoDecimalPlaces(BigDecimal amount) {
        if (amount == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount is required");
        }
        try {
            amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount supports at most 2 decimal places");
        }
    }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount is required");
        }
        requireAtMostTwoDecimalPlaces(amount);
        return amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static void requirePositive(BigDecimal amount) {
        BigDecimal n = normalize(amount);
        if (n.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
    }
}
