package com.drippay.service;

import com.drippay.domain.entity.Budget;
import com.drippay.domain.entity.TransactionCategory;
import com.drippay.domain.entity.User;
import com.drippay.domain.entity.Wallet;
import com.drippay.domain.entity.WalletTransaction;
import com.drippay.dto.insights.PayeeContactResponse;
import com.drippay.dto.insights.PayeeContactsResponse;
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
 * SpendSense — rule-based spending insights from persisted transactions.
 */
@Service
public class InsightService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public InsightService(WalletRepository walletRepository,
                          WalletTransactionRepository transactionRepository,
                          TransactionCategoryRepository categoryRepository,
                          BudgetRepository budgetRepository,
                          UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));

        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        BigDecimal sent = transactionRepository.sumSentSettled(wallet.getId(), from, to);
        BigDecimal received = transactionRepository.sumReceivedSettled(wallet.getId(), from, to);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("balance", wallet.getBalance());
        summary.put("currency", "INR");
        summary.put("monthlySent", sent);
        summary.put("monthlyReceived", received);
        summary.put("monthlySpending", sent);
        summary.put("insights", buildInsights(userId, wallet.getId(), month));
        return summary;
    }

    @Transactional(readOnly = true)
    public PayeeContactsResponse payeeContacts(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));

        List<WalletTransaction> outbound = transactionRepository.findRecentOutboundP2P(
                wallet.getId(), PageRequest.of(0, 150));

        Map<Long, PayeeStats> statsByRecipient = new LinkedHashMap<>();
        for (WalletTransaction tx : outbound) {
            Long recipientId = tx.getRecipientWalletId();
            if (recipientId == null) {
                continue;
            }
            PayeeStats stats = statsByRecipient.computeIfAbsent(recipientId, id -> new PayeeStats());
            stats.transferCount++;
            if (stats.lastPaidAt == null || tx.getCreatedAt().isAfter(stats.lastPaidAt)) {
                stats.lastPaidAt = tx.getCreatedAt();
                stats.lastAmount = tx.getAmount();
            }
        }

        List<PayeeContactResponse> recentlyPaid = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (WalletTransaction tx : outbound) {
            Long recipientId = tx.getRecipientWalletId();
            if (recipientId == null || seen.contains(recipientId)) {
                continue;
            }
            seen.add(recipientId);
            recentlyPaid.add(toPayeeContact(recipientId, statsByRecipient.get(recipientId)));
            if (recentlyPaid.size() >= 5) {
                break;
            }
        }

        List<PayeeContactResponse> mostContacted = statsByRecipient.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().transferCount, a.getValue().transferCount))
                .limit(5)
                .map(e -> toPayeeContact(e.getKey(), e.getValue()))
                .toList();

        return new PayeeContactsResponse(recentlyPaid, mostContacted);
    }

    private PayeeContactResponse toPayeeContact(Long recipientWalletId, PayeeStats stats) {
        Wallet recipientWallet = walletRepository.findById(recipientWalletId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient wallet not found"));
        User user = userRepository.findById(recipientWallet.getUserId()).orElse(null);
        String displayName = user != null ? user.getFullName() : recipientWallet.getPublicWalletId();
        String dripHandle = user != null ? user.getDripHandle() : null;
        return new PayeeContactResponse(
                recipientWallet.getPublicWalletId(),
                displayName,
                dripHandle,
                stats.lastPaidAt,
                stats.lastAmount,
                stats.transferCount);
    }

    private static final class PayeeStats {
        Instant lastPaidAt;
        BigDecimal lastAmount;
        long transferCount;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> categoryBreakdown(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));

        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        var page = transactionRepository.searchForWallet(wallet.getId(), null, from, to,
                org.springframework.data.domain.Pageable.unpaged());

        Map<Long, BigDecimal> totals = new HashMap<>();
        for (WalletTransaction tx : page) {
            if (tx.getSenderWalletId() != null && tx.getSenderWalletId().equals(wallet.getId())) {
                Long catId = tx.getCategoryId() != null ? tx.getCategoryId() : -1L;
                totals.merge(catId, tx.getAmount(), BigDecimal::add);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : totals.entrySet()) {
            String label = "Other";
            String code = "OTHER";
            if (entry.getKey() > 0) {
                TransactionCategory cat = categoryRepository.findById(entry.getKey()).orElse(null);
                if (cat != null) {
                    label = cat.getLabel();
                    code = cat.getCode();
                }
            }
            result.add(Map.of("categoryCode", code, "label", label, "amount", entry.getValue()));
        }
        result.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        return result;
    }

    private List<String> buildInsights(Long userId, Long walletId, YearMonth month) {
        List<String> insights = new ArrayList<>();
        String monthYear = month.toString();

        List<Budget> budgets = budgetRepository.findByUserIdAndMonthYear(userId, monthYear);
        Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        for (Budget budget : budgets) {
            var txs = transactionRepository.searchForWallet(walletId, budget.getCategoryId(), from, to,
                    org.springframework.data.domain.Pageable.unpaged());
            BigDecimal spent = txs.getContent().stream()
                    .filter(t -> walletId.equals(t.getSenderWalletId()))
                    .map(WalletTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            TransactionCategory cat = categoryRepository.findById(budget.getCategoryId()).orElse(null);
            String label = cat != null ? cat.getLabel() : "category";
            insights.add(String.format("You've spent ₹%s of your ₹%s %s budget this month.",
                    spent.setScale(2), budget.getMonthlyLimit().setScale(2), label));

            if (spent.compareTo(budget.getMonthlyLimit()) >= 0) {
                insights.add("Budget alert: " + label + " budget reached or exceeded (rule: budget_threshold).");
            }
        }

        // Previous period comparison (simple)
        YearMonth prev = month.minusMonths(1);
        Instant pFrom = prev.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant pTo = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal thisMonth = transactionRepository.sumSentSettled(walletId, from, to);
        BigDecimal lastMonth = transactionRepository.sumSentSettled(walletId, pFrom, pTo);
        if (lastMonth.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal change = thisMonth.subtract(lastMonth);
            insights.add("You spent ₹" + change.setScale(2) + " more than last month on outgoing transfers (rule: period_compare).");
        }

        var categories = categoryBreakdown(userId);
        if (!categories.isEmpty()) {
            Map<String, Object> top = categories.get(0);
            insights.add("Highest spending category this month: " + top.get("label") + " (rule: top_category).");
        }

        return insights;
    }
}
