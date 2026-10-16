package com.drippay.controller;

import com.drippay.dto.security.SecurityAlertResponse;
import com.drippay.repository.SecurityAlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final SecurityAlertRepository alertRepository;

    public AdminController(SecurityAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping("/alerts")
    public List<SecurityAlertResponse> allAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(a -> new SecurityAlertResponse(
                        a.getId(), a.getSeverity(), a.getReason(), a.getReviewStatus(), a.getCreatedAt()))
                .toList();
    }
}
