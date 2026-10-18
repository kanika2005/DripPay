package com.drippay.controller;

import com.drippay.dto.transaction.TransferRequest;
import com.drippay.dto.transaction.TransactionResponse;
import com.drippay.security.SecurityUtils;
import com.drippay.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * SafeTransfer — Idempotency-Key header is required for duplicate-safe transfers.
     */
    @PostMapping
    public TransactionResponse transfer(@Valid @RequestBody TransferRequest request,
                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                      HttpServletRequest http) {
        return transferService.safeTransfer(
                SecurityUtils.currentUser().getId(), request, idempotencyKey, http.getRemoteAddr());
    }
}
