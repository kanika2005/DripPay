package com.drippay.service;

import com.drippay.config.DrippayProperties;
import com.drippay.domain.entity.SecurityAlert;
import com.drippay.domain.enums.AlertSeverity;
import com.drippay.domain.enums.TransactionStatus;
import com.drippay.domain.enums.TransactionType;
import com.drippay.repository.SecurityAlertRepository;
import com.drippay.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * FraudWatch — explainable rule-based alerts (not confirmed fraud).
 */
@Service
public class FraudWatchService {

    private final SecurityAlertRepository alertRepository;
    private final WalletTransactionRepository transactionRepository;
    private final DrippayProperties properties;

    public FraudWatchService(SecurityAlertRepository alertRepository,
                             WalletTransactionRepository transactionRepository,
                             DrippayProperties properties) {
        this.alertRepository = alertRepository;
        this.transactionRepository = transactionRepository;
        this.properties = properties;
    }

    @Transactional
    public void onFailedTransfer(Long userId) {
        Instant hourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long failed = transactionRepository.countByInitiatedByUserIdAndStatusAndTypeAndCreatedAtAfter(
                userId, TransactionStatus.FAILED, TransactionType.P2P_TRANSFER, hourAgo);
        // Note: we throw before persisting FAILED rows today; count may be low — also track via audit in future.
        if (failed >= properties.getFraud().getMaxFailedTransfersPerHour()) {
            createAlert(userId, AlertSeverity.MEDIUM,
                    "Multiple failed transfer attempts detected in the last hour (rule: failed_attempts).");
        }
    }

    @Transactional
    public void onSuccessfulTransfer(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(properties.getFraud().getHighAmountThreshold())) > 0) {
            createAlert(userId, AlertSeverity.MEDIUM,
                    "Transfer amount exceeded configured high-amount threshold (rule: high_amount).");
        }

        Instant windowStart = Instant.now().minus(properties.getFraud().getRapidTxWindowMinutes(), ChronoUnit.MINUTES);
        long count = transactionRepository.countByInitiatedByUserIdAndCreatedAtAfter(userId, windowStart);
        if (count >= properties.getFraud().getRapidTxCount()) {
            createAlert(userId, AlertSeverity.HIGH,
                    "Unusually frequent transactions in a short window (rule: rapid_transactions).");
        }
    }

    private void createAlert(Long userId, AlertSeverity severity, String reason) {
        SecurityAlert alert = new SecurityAlert();
        alert.setUserId(userId);
        alert.setSeverity(severity);
        alert.setReason(reason);
        alertRepository.save(alert);
    }
}
