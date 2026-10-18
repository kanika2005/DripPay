package com.drippay.service;

import com.drippay.domain.entity.Budget;
import com.drippay.dto.budget.BudgetRequest;
import com.drippay.dto.budget.BudgetResponse;
import com.drippay.exception.ApiException;
import com.drippay.repository.BudgetRepository;
import com.drippay.repository.TransactionCategoryRepository;
import com.drippay.repository.WalletRepository;
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
    private final WalletRepository walletRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         TransactionCategoryRepository categoryRepository,
                         WalletRepository walletRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.walletRepository = walletRepository;
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

        var limit = MoneyUtils.normalize(request.monthlyLimit());
        var balance = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"))
                .getBalance();
        if (limit.compareTo(balance) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Budget amount is more than your wallet balance. Available: " + balance);
        }

        String monthYear = request.monthYear() != null ? request.monthYear()
                : YearMonth.now(ZoneOffset.UTC).toString();

        Budget budget = budgetRepository.findByUserIdAndCategoryIdAndMonthYear(userId, categoryId, monthYear)
                .orElseGet(Budget::new);
        budget.setUserId(userId);
        budget.setCategoryId(categoryId);
        budget.setMonthYear(monthYear);
        budget.setMonthlyLimit(limit);
        return toResponse(budgetRepository.save(budget));
    }

    private BudgetResponse toResponse(Budget b) {
        String code = categoryRepository.findById(b.getCategoryId()).map(c -> c.getCode()).orElse("OTHER");
        return new BudgetResponse(b.getId(), code, b.getMonthYear(), b.getMonthlyLimit());
    }
}
