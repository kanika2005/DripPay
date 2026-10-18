package com.drippay.controller;

import com.drippay.security.SecurityUtils;
import com.drippay.ai.InsightsChatService;
import com.drippay.dto.insights.ChatRequest;
import com.drippay.dto.insights.ChatResponse;
import com.drippay.service.InsightService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/insights")
public class InsightController {

    private final InsightService insightService;
    private final InsightsChatService insightsChatService;

    public InsightController(InsightService insightService, InsightsChatService insightsChatService) {
        this.insightService = insightService;
        this.insightsChatService = insightsChatService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return insightService.dashboardSummary(SecurityUtils.currentUser().getId());
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> categories() {
        return insightService.categoryBreakdown(SecurityUtils.currentUser().getId());
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return insightsChatService.chat(SecurityUtils.currentUser().getId(), request.message());
    }
}
