package com.drippay.service;

import com.drippay.domain.entity.Budget;
import com.drippay.dto.budget.BudgetRequest;
import com.drippay.dto.budget.BudgetResponse;
import com.drippay.exception.ApiException;
import com.drippay.repository.BudgetRepository;
import com.drippay.repository.TransactionCategoryRepository;
import com.drippay.util.MoneyUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionCategoryRepository categoryRepository;

    public BudgetService(BudgetRepository budgetRepository, TransactionCategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> listForCurrentMonth(Long userId) {
        String monthYear = YearMonth.now(ZoneOffset.UTC).toString();
        return budgetRepository.findByUserIdAndMonthYear(userId, monthYear).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BudgetResponse upsert(Long userId, BudgetRequest request) {
        Long categoryId = categoryRepository.findByCode(request.categoryCode().toUpperCase())
                .map(c -> c.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown category"));

        String monthYear = request.monthYear() != null ? request.monthYear()
                : YearMonth.now(ZoneOffset.UTC).toString();

        Budget budget = budgetRepository.findByUserIdAndCategoryIdAndMonthYear(userId, categoryId, monthYear)
                .orElseGet(Budget::new);
        budget.setUserId(userId);
        budget.setCategoryId(categoryId);
        budget.setMonthYear(monthYear);
        budget.setMonthlyLimit(MoneyUtils.normalize(request.monthlyLimit()));
        return toResponse(budgetRepository.save(budget));
    }

    private BudgetResponse toResponse(Budget b) {
        String code = categoryRepository.findById(b.getCategoryId()).map(c -> c.getCode()).orElse("OTHER");
        return new BudgetResponse(b.getId(), code, b.getMonthYear(), b.getMonthlyLimit());
    }
}
