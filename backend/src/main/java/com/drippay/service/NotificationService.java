package com.drippay.service;

import com.drippay.domain.entity.Notification;
import com.drippay.domain.enums.NotificationType;
import com.drippay.dto.notification.NotificationResponse;
import com.drippay.exception.ApiException;
import com.drippay.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifyUser(Long userId, NotificationType type, String title, String body,
                           Long groupId, Long groupExpenseId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setGroupId(groupId);
        n.setGroupExpenseId(groupExpenseId);
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(Long userId) {
        return notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        n.setReadAt(Instant.now());
        return toResponse(notificationRepository.save(n));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getGroupId(),
                n.getGroupExpenseId(),
                n.getReadAt() != null,
                n.getCreatedAt()
        );
    }
}
