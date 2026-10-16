package com.drippay.controller;

import com.drippay.security.SecurityUtils;
import com.drippay.service.InsightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/insights")
public class InsightController {

    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return insightService.dashboardSummary(SecurityUtils.currentUser().getId());
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> categories() {
        return insightService.categoryBreakdown(SecurityUtils.currentUser().getId());
    }
}
