package com.drippay.controller;

import com.drippay.domain.entity.SecurityAlert;
import com.drippay.domain.enums.ReviewStatus;
import com.drippay.dto.security.SecurityAlertResponse;
import com.drippay.exception.ApiException;
import com.drippay.repository.SecurityAlertRepository;
import com.drippay.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/security")
public class SecurityAlertController {

    private final SecurityAlertRepository alertRepository;

    public SecurityAlertController(SecurityAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping("/alerts")
    public List<SecurityAlertResponse> myAlerts() {
        Long userId = SecurityUtils.currentUser().getId();
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PatchMapping("/alerts/{alertId}/review")
    public SecurityAlertResponse review(@PathVariable Long alertId, @RequestBody ReviewBody body) {
        SecurityAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alert not found"));

        if (!alert.getUserId().equals(SecurityUtils.currentUser().getId())
                && !SecurityUtils.currentUser().getRole().name().equals("ADMIN")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        alert.setReviewStatus(body.status());
        alert.setReviewedAt(Instant.now());
        alert.setReviewedByUserId(SecurityUtils.currentUser().getId());
        return toResponse(alertRepository.save(alert));
    }

    private SecurityAlertResponse toResponse(SecurityAlert a) {
        return new SecurityAlertResponse(a.getId(), a.getSeverity(), a.getReason(), a.getReviewStatus(), a.getCreatedAt());
    }

    public record ReviewBody(ReviewStatus status) {
    }
}
