package com.drippay.controller;

import com.drippay.dto.transaction.AmountRequest;
import com.drippay.dto.transaction.TransactionResponse;
import com.drippay.dto.wallet.WalletLookupResponse;
import com.drippay.dto.wallet.WalletResponse;
import com.drippay.security.SecurityUtils;
import com.drippay.service.TransferService;
import com.drippay.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;
    private final TransferService transferService;

    public WalletController(WalletService walletService, TransferService transferService) {
        this.walletService = walletService;
        this.transferService = transferService;
    }

    @GetMapping("/me")
    public WalletResponse myWallet() {
        return walletService.getMyWallet(SecurityUtils.currentUser().getId());
    }

    @GetMapping("/lookup/{walletId}")
    public WalletLookupResponse lookup(@PathVariable String walletId) {
        return walletService.lookup(walletId, SecurityUtils.currentUser().getId());
    }

    @PostMapping("/demo-deposits")
    public TransactionResponse demoDeposit(@Valid @RequestBody AmountRequest request, HttpServletRequest http) {
        return transferService.demoDeposit(SecurityUtils.currentUser().getId(), request, http.getRemoteAddr());
    }

    @PostMapping("/demo-withdrawals")
    public TransactionResponse demoWithdraw(@Valid @RequestBody AmountRequest request, HttpServletRequest http) {
        return transferService.demoWithdraw(SecurityUtils.currentUser().getId(), request, http.getRemoteAddr());
    }
}
