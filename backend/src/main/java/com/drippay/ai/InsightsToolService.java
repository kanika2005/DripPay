package com.drippay.ai;

import com.drippay.domain.entity.Budget;
import com.drippay.domain.entity.TransactionCategory;
import com.drippay.domain.entity.Wallet;
import com.drippay.domain.entity.WalletTransaction;
import com.drippay.domain.enums.TransactionStatus;
import com.drippay.exception.ApiException;
import com.drippay.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Read-only tools for the insights agent — always scoped to the authenticated user id.
 */
@Service
public class InsightsToolService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;

    public InsightsToolService(WalletRepository walletRepository,
                               WalletTransactionRepository transactionRepository,
                               TransactionCategoryRepository categoryRepository,
                               BudgetRepository budgetRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal getMonthlySpending(Long userId, String categoryCode) {
        Wallet wallet = walletFor(userId);
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Long categoryId = null;
        if (categoryCode != null && !categoryCode.isBlank()) {
            categoryId = categoryRepository.findByCode(categoryCode.toUpperCase())
                    .map(TransactionCategory::getId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown category"));
        }
        var page = transactionRepository.searchForWallet(wallet.getId(), categoryId, from, to, PageRequest.of(0, 500));
        return page.getContent().stream()
                .filter(t -> wallet.getId().equals(t.getSenderWalletId()))
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> compareSpending(Long userId) {
        Wallet wallet = walletFor(userId);
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        YearMonth prev = month.minusMonths(1);
        Instant thisFrom = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant thisTo = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant prevFrom = prev.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant prevTo = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return Map.of(
                "thisMonth", transactionRepository.sumSentSettled(wallet.getId(), thisFrom, thisTo),
                "lastMonth", transactionRepository.sumSentSettled(wallet.getId(), prevFrom, prevTo)
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategoryBreakdown(Long userId) {
        Wallet wallet = walletFor(userId);
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        var page = transactionRepository.searchForWallet(wallet.getId(), null, from, to, PageRequest.of(0, 500));
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (WalletTransaction tx : page) {
            if (wallet.getId().equals(tx.getSenderWalletId())) {
                totals.merge(tx.getCategoryId() != null ? tx.getCategoryId() : -1L, tx.getAmount(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (var e : totals.entrySet()) {
            String label = "Other";
            String code = "OTHER";
            if (e.getKey() > 0) {
                TransactionCategory cat = categoryRepository.findById(e.getKey()).orElse(null);
                if (cat != null) {
                    label = cat.getLabel();
                    code = cat.getCode();
                }
            }
            out.add(Map.of("categoryCode", code, "label", label, "amount", e.getValue()));
        }
        out.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        return out.size() > 10 ? out.subList(0, 10) : out;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBudgetStatus(Long userId) {
        String monthYear = YearMonth.now(ZoneOffset.UTC).toString();
        Wallet wallet = walletFor(userId);
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthYear(userId, monthYear);
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Budget b : budgets) {
            var txs = transactionRepository.searchForWallet(wallet.getId(), b.getCategoryId(), from, to, PageRequest.of(0, 200));
            BigDecimal spent = txs.getContent().stream()
                    .filter(t -> wallet.getId().equals(t.getSenderWalletId()))
                    .map(WalletTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String label = categoryRepository.findById(b.getCategoryId()).map(TransactionCategory::getLabel).orElse("Category");
            result.add(Map.of(
                    "label", label,
                    "limit", b.getMonthlyLimit(),
                    "spent", spent,
                    "percentUsed", b.getMonthlyLimit().signum() == 0 ? 0
                            : spent.multiply(BigDecimal.valueOf(100)).divide(b.getMonthlyLimit(), 2, java.math.RoundingMode.HALF_UP)
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRecentTransactions(Long userId, int limit) {
        Wallet wallet = walletFor(userId);
        int capped = Math.min(Math.max(limit, 1), 20);
        return transactionRepository.searchForWallet(wallet.getId(), null, null, null, PageRequest.of(0, capped))
                .map(tx -> {
                    String dir = wallet.getId().equals(tx.getSenderWalletId()) ? "OUT" : "IN";
                    return Map.<String, Object>of(
                            "referenceId", tx.getReferenceId(),
                            "amount", tx.getAmount(),
                            "direction", dir,
                            "status", tx.getStatus().name(),
                            "description", tx.getDescription() != null ? tx.getDescription() : ""
                    );
                }).getContent();
    }

    @Transactional(readOnly = true)
    public Optional<String> explainFailedPayment(Long userId, String referenceId) {
        Wallet wallet = walletFor(userId);
        WalletTransaction tx = transactionRepository.findByReferenceId(referenceId)
                .orElse(null);
        if (tx == null || !wallet.getId().equals(tx.getSenderWalletId()) && !wallet.getId().equals(tx.getRecipientWalletId())) {
            return Optional.empty();
        }
        if (tx.getStatus() == TransactionStatus.COMPLETED || tx.getStatus() == TransactionStatus.SUCCESS) {
            return Optional.of("Payment " + referenceId + " completed successfully.");
        }
        if (tx.getStatus() == TransactionStatus.REVERSED) {
            return Optional.of("Payment was reversed — funds were returned via a compensating transfer.");
        }
        return Optional.of("Payment status is " + tx.getStatus() + ". Check balance and limits, or retry with a new idempotency key.");
    }

    private Wallet walletFor(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }
}
