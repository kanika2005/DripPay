package com.drippay.controller;

import com.drippay.dto.budget.BudgetRequest;
import com.drippay.dto.budget.BudgetResponse;
import com.drippay.security.SecurityUtils;
import com.drippay.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetResponse> list() {
        return budgetService.listForCurrentMonth(SecurityUtils.currentUser().getId());
    }

    @PostMapping
    public BudgetResponse create(@Valid @RequestBody BudgetRequest request) {
        return budgetService.upsert(SecurityUtils.currentUser().getId(), request);
    }
}
