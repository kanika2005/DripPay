package com.drippay.service;

import com.drippay.domain.entity.AuditLog;
import com.drippay.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(Long userId, String action, String details, String ip) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDetails(details);
        log.setIpAddress(ip);
        auditLogRepository.save(log);
    }
}
