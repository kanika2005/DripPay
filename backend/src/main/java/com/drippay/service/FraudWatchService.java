package com.drippay.service;

import com.drippay.config.DrippayProperties;
import com.drippay.domain.entity.SecurityAlert;
import com.drippay.domain.enums.AlertSeverity;
import com.drippay.domain.enums.TransactionStatus;
import com.drippay.domain.enums.TransactionType;
import com.drippay.repository.SecurityAlertRepository;
import com.drippay.repository.WalletTransactionRepository;
import com.drippay.risk.RiskRule;
import com.drippay.risk.RiskRuleConfig;
import com.drippay.risk.RiskRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final RiskRuleRepository riskRuleRepository;
    private final DrippayProperties properties;
    private final ObjectMapper objectMapper;

    public FraudWatchService(SecurityAlertRepository alertRepository,
                             WalletTransactionRepository transactionRepository,
                             RiskRuleRepository riskRuleRepository,
                             DrippayProperties properties,
                             ObjectMapper objectMapper) {
        this.alertRepository = alertRepository;
        this.transactionRepository = transactionRepository;
        this.riskRuleRepository = riskRuleRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void onFailedTransfer(Long userId) {
        if (!isEnabled("FAILED_ATTEMPTS")) {
            return;
        }
        JsonNode cfg = configFor("FAILED_ATTEMPTS");
        int maxPerHour = RiskRuleConfig.intValue(cfg, "maxPerHour",
                properties.getFraud().getMaxFailedTransfersPerHour());
        Instant hourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long failed = transactionRepository.countByInitiatedByUserIdAndStatusAndTypeAndCreatedAtAfter(
                userId, TransactionStatus.FAILED, TransactionType.P2P_TRANSFER, hourAgo);
        if (failed >= maxPerHour) {
            createAlert(userId, AlertSeverity.MEDIUM,
                    ruleDescription("FAILED_ATTEMPTS",
                            "repeated failed transfer attempts in the last hour (rule: failed_attempts)."));
        }
    }

    @Transactional
    public void onSuccessfulTransfer(Long userId, BigDecimal amount) {
        if (isEnabled("HIGH_AMOUNT")) {
            JsonNode cfg = configFor("HIGH_AMOUNT");
            long threshold = cfg.has("threshold")
                    ? cfg.get("threshold").asLong()
                    : properties.getFraud().getHighAmountThreshold();
            if (amount.compareTo(BigDecimal.valueOf(threshold)) > 0) {
                createAlert(userId, AlertSeverity.MEDIUM,
                        ruleDescription("HIGH_AMOUNT",
                                "transfer amount above configured threshold (rule: high_amount)."));
            }
        }

        if (isEnabled("VELOCITY_TRANSFER")) {
            JsonNode cfg = configFor("VELOCITY_TRANSFER");
            int windowMinutes = RiskRuleConfig.intValue(cfg, "windowMinutes",
                    properties.getFraud().getRapidTxWindowMinutes());
            int maxCount = RiskRuleConfig.intValue(cfg, "maxCount",
                    properties.getFraud().getRapidTxCount());
            Instant windowStart = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);
            long count = transactionRepository.countByInitiatedByUserIdAndStatusAndTypeAndCreatedAtAfter(
                    userId, TransactionStatus.SUCCESS, TransactionType.P2P_TRANSFER, windowStart);
            if (count >= maxCount) {
                createAlert(userId, AlertSeverity.HIGH,
                        ruleDescription("VELOCITY_TRANSFER",
                                "many transfers in a short window (rule: velocity_transfer)."));
            }
        }
    }

    private boolean isEnabled(String code) {
        return riskRuleRepository.findByCode(code)
                .map(RiskRule::isEnabled)
                .orElse(true);
    }

    private JsonNode configFor(String code) {
        return riskRuleRepository.findByCode(code)
                .map(RiskRule::getConfigJson)
                .map(json -> RiskRuleConfig.parse(json, objectMapper))
                .orElse(objectMapper.createObjectNode());
    }

    private String ruleDescription(String code, String detail) {
        String prefix = riskRuleRepository.findByCode(code)
                .map(RiskRule::getDescription)
                .orElse("Potentially suspicious activity");
        if (!prefix.toLowerCase().contains("potentially suspicious")) {
            prefix = "Potentially suspicious: " + prefix;
        }
        return prefix + " " + detail;
    }

    private void createAlert(Long userId, AlertSeverity severity, String reason) {
        SecurityAlert alert = new SecurityAlert();
        alert.setUserId(userId);
        alert.setSeverity(severity);
        alert.setReason(reason);
        alertRepository.save(alert);
    }
}
