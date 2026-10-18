package com.drippay.controller;

import com.drippay.dto.common.PageResponse;
import com.drippay.dto.transaction.TransactionResponse;
import com.drippay.security.SecurityUtils;
import com.drippay.service.TransferService;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransferService transferService;

    public TransactionController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping
    public PageResponse<TransactionResponse> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(transferService.listTransactions(
                SecurityUtils.currentUser().getId(), categoryId, from, to, PageRequest.of(page, size)));
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse get(@PathVariable Long transactionId) {
        return transferService.getTransaction(SecurityUtils.currentUser().getId(), transactionId);
    }

    @GetMapping("/reference/{referenceId}")
    public TransactionResponse byReference(@PathVariable String referenceId) {
        return transferService.getByReference(SecurityUtils.currentUser().getId(), referenceId);
    }

    @PostMapping("/{transactionId}/reverse")
    public TransactionResponse reverse(@PathVariable Long transactionId, HttpServletRequest http) {
        return transferService.reverseTransfer(
                SecurityUtils.currentUser().getId(), transactionId, http.getRemoteAddr());
    }
}
