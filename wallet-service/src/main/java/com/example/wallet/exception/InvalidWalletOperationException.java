package com.example.wallet.exception;

/**
 * Thrown for business rule violations (e.g. insufficient balance, amount <= 0).
 * GlobalExceptionHandler maps this to HTTP 400.
 */
public class InvalidWalletOperationException extends RuntimeException {

    public InvalidWalletOperationException(String message) {
        super(message);
    }
}
