package com.drippay.dto.security;

import com.drippay.domain.enums.AlertSeverity;
import com.drippay.domain.enums.ReviewStatus;

import java.time.Instant;

public record SecurityAlertResponse(
        Long id,
        AlertSeverity severity,
        String reason,
        ReviewStatus reviewStatus,
        Instant createdAt
) {
}
