package com.drippay.dto.notification;

import com.drippay.domain.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        Long groupId,
        Long groupExpenseId,
        boolean read,
        Instant createdAt
) {
}
