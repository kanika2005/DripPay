package com.drippay.payment;

import com.drippay.dto.payment.RazorpayOrderResponse;
import com.drippay.dto.payment.RazorpayVerifyRequest;
import com.drippay.dto.transaction.TransactionResponse;
import com.drippay.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payments/razorpay")
public class RazorpayController {

    private final RazorpayService razorpayService;

    public RazorpayController(RazorpayService razorpayService) {
        this.razorpayService = razorpayService;
    }

    @PostMapping("/orders")
    public RazorpayOrderResponse createOrder(@RequestBody CreateOrderBody body) {
        return razorpayService.createOrder(SecurityUtils.currentUser().getId(), body.amount());
    }

    @PostMapping("/verify")
    public TransactionResponse verify(@Valid @RequestBody RazorpayVerifyRequest body, HttpServletRequest http) {
        return razorpayService.verifyAndCredit(SecurityUtils.currentUser().getId(), body, http.getRemoteAddr());
    }

    public record CreateOrderBody(@NotNull @DecimalMin("1.00") BigDecimal amount) {
    }
}
