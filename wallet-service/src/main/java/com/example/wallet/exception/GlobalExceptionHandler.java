package com.example.wallet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler.
 *
 * Flow:
 *   Service throws WalletNotFoundException
 *        ↓
 *   @RestControllerAdvice catches it
 *        ↓
 *   returns HTTP 404 JSON body
 *
 * What is @RestControllerAdvice?
 * - A special Spring component that watches ALL controllers
 * - When an exception escapes a controller/service call, matching
 *   @ExceptionHandler methods run
 *
 * Without it:
 * - Spring returns a default HTML/JSON error page
 * - You cannot control the status code / body easily in one place
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWalletNotFound(WalletNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(404, ex.getMessage()));
    }

    @ExceptionHandler(InvalidWalletOperationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOperation(InvalidWalletOperationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(400, ex.getMessage()));
    }

    private Map<String, Object> errorBody(int status, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status,
                "message", message
        );
    }
}
