package com.example.wallet.exception;

/**
 * Thrown when a wallet id does not exist in the database.
 * GlobalExceptionHandler maps this to HTTP 404.
 */
public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(Long walletId) {
        super("Wallet not found with id: " + walletId);
    }
}
