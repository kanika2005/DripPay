package com.drippay.payment;

import com.drippay.dto.payment.PaymentDraftResponse;
import com.drippay.dto.transaction.TransactionResponse;
import com.drippay.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payment-drafts")
public class PaymentDraftController {

    private final PaymentDraftService paymentDraftService;

    public PaymentDraftController(PaymentDraftService paymentDraftService) {
        this.paymentDraftService = paymentDraftService;
    }

    @PostMapping
    public PaymentDraftResponse create(@RequestBody CreateDraftBody body) {
        return paymentDraftService.create(
                SecurityUtils.currentUser().getId(),
                body.recipientHandle(),
                body.amount(),
                body.note(),
                "USER");
    }

    @GetMapping("/{id}")
    public PaymentDraftResponse get(@PathVariable Long id) {
        return paymentDraftService.get(SecurityUtils.currentUser().getId(), id);
    }

    @PostMapping("/{id}/confirm")
    public TransactionResponse confirm(@PathVariable Long id,
                                     @RequestBody ConfirmDraftBody body,
                                     HttpServletRequest http) {
        return paymentDraftService.confirm(
                SecurityUtils.currentUser().getId(), id, body.pin(), http.getRemoteAddr());
    }

    public record CreateDraftBody(
            @NotBlank String recipientHandle,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String note
    ) {
    }

    public record ConfirmDraftBody(@NotBlank String pin) {
    }
}
