package com.drippay.controller;

import com.drippay.ai.FraudExplainerService;
import com.drippay.dto.security.SecurityAlertResponse;
import com.drippay.reconciliation.ReconciliationService;
import com.drippay.repository.SecurityAlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final SecurityAlertRepository alertRepository;
    private final ReconciliationService reconciliationService;
    private final FraudExplainerService fraudExplainerService;

    public AdminController(SecurityAlertRepository alertRepository,
                           ReconciliationService reconciliationService,
                           FraudExplainerService fraudExplainerService) {
        this.alertRepository = alertRepository;
        this.reconciliationService = reconciliationService;
        this.fraudExplainerService = fraudExplainerService;
    }

    @GetMapping("/alerts")
    public List<SecurityAlertResponse> allAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(a -> new SecurityAlertResponse(
                        a.getId(), a.getSeverity(), a.getReason(), a.getReviewStatus(), a.getCreatedAt()))
                .toList();
    }

    @GetMapping("/alerts/{alertId}/explain")
    public Map<String, String> explainAlert(@PathVariable Long alertId) {
        var alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new com.drippay.exception.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Alert not found"));
        return Map.of("explanation", fraudExplainerService.explainForAdmin(alert));
    }

    @GetMapping("/reconciliation")
    public ReconciliationService.ReconciliationReport reconciliation() {
        return reconciliationService.getLastReport();
    }

    @PostMapping("/reconciliation/run")
    public ReconciliationService.ReconciliationReport runReconciliation() {
        return reconciliationService.runCheck();
    }
}
