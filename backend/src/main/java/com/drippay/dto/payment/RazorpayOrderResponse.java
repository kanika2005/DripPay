package com.drippay.dto.payment;

public record RazorpayOrderResponse(
        String orderId,
        String keyId,
        long amountPaise,
        String currency,
        boolean simulated
) {
}
