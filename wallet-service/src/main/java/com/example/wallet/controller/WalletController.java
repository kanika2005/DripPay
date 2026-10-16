package com.example.wallet.controller;

import com.example.wallet.dto.AddMoneyRequest;
import com.example.wallet.dto.CreateWalletRequest;
import com.example.wallet.dto.TransactionResponse;
import com.example.wallet.dto.WalletResponse;
import com.example.wallet.dto.WithdrawMoneyRequest;
import com.example.wallet.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller = entry point for HTTP requests.
 *
 * What is @RestController?
 * ------------------------
 * Combines:
 *   @Controller  → this class handles web requests
 *   @ResponseBody → return values become JSON (not HTML view names)
 *
 * What is @RequestMapping("/api/v1/wallets")?
 * - Base path for all methods in this class
 *
 * Controllers should be THIN:
 *   receive request → call service → return response
 * No business logic here (no balance math, no DB rules).
 */
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * POST /api/v1/wallets
     * Body: { "userId": 101 }
     */
    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@RequestBody CreateWalletRequest request) {
        WalletResponse response = walletService.createWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/wallets/{walletId}
     * Also acts as "check balance" — balance is inside the response.
     */
    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long walletId) {
        return ResponseEntity.ok(walletService.getWallet(walletId));
    }

    /**
     * POST /api/v1/wallets/{walletId}/credit
     * Body: { "amount": 500 }
     */
    @PostMapping("/{walletId}/credit")
    public ResponseEntity<WalletResponse> addMoney(@PathVariable Long walletId,
                                                   @RequestBody AddMoneyRequest request) {
        return ResponseEntity.ok(walletService.addMoney(walletId, request));
    }

    /**
     * POST /api/v1/wallets/{walletId}/debit
     * Body: { "amount": 200 }
     */
    @PostMapping("/{walletId}/debit")
    public ResponseEntity<WalletResponse> withdrawMoney(@PathVariable Long walletId,
                                                        @RequestBody WithdrawMoneyRequest request) {
        return ResponseEntity.ok(walletService.withdrawMoney(walletId, request));
    }

    /**
     * GET /api/v1/wallets/{walletId}/transactions
     */
    @GetMapping("/{walletId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable Long walletId) {
        return ResponseEntity.ok(walletService.getTransactions(walletId));
    }
}
