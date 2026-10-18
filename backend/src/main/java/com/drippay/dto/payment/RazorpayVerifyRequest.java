package com.drippay.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record RazorpayVerifyRequest(
        @NotBlank String orderId,
        String paymentId,
        String signature
) {
}
