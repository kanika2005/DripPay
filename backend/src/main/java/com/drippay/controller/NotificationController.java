package com.drippay.controller;

import com.drippay.dto.notification.NotificationResponse;
import com.drippay.security.SecurityUtils;
import com.drippay.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return notificationService.listForUser(SecurityUtils.currentUser().getId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        long count = notificationService.unreadCount(SecurityUtils.currentUser().getId());
        return Map.of("count", count);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(SecurityUtils.currentUser().getId(), id);
    }
}
