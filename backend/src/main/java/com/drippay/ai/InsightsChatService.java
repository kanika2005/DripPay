package com.drippay.ai;

import com.drippay.dto.insights.ChatResponse;
import com.drippay.dto.payment.PaymentDraftResponse;
import com.drippay.payment.PaymentDraftService;
import com.drippay.ratelimit.RateLimitService;
import com.drippay.repository.TransactionCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InsightsChatService {

    private static final Logger log = LoggerFactory.getLogger(InsightsChatService.class);
    private static final int MAX_REPLY_LEN = 1500;
    private static final Pattern DRAFT_PATTERN = Pattern.compile(
            "(?:send|pay|transfer)\\s+(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d{1,4})?)\\s+to\\s+([a-z0-9._-]+@drippay)",
            Pattern.CASE_INSENSITIVE);

    private static final String CAPABILITIES_REPLY =
            "I can help with: monthly spending by category, budget status, compare to last month, "
                    + "recent transactions, or why a payment failed (include reference). "
                    + "To pay someone, say: send 100 to name@drippay — I'll create a draft you confirm with your PIN.";

    private static final String APP_OVERVIEW_REPLY =
            "DripPay is a simulated wallet app: Dashboard, Collect (name@drippay QR), Transfers, "
                    + "Razorpay test top-up, statements (CSV/PDF), Budgets, Groups, and this Assistant. "
                    + "Money is demo-only. I read your ledger; I cannot move money without a PIN-confirmed draft.";

    private final InsightsToolService tools;
    private final AgentConversationRepository conversationRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final RateLimitService rateLimitService;
    private final LlmInsightsClient llmInsightsClient;
    private final PaymentDraftService paymentDraftService;

    public InsightsChatService(InsightsToolService tools,
                               AgentConversationRepository conversationRepository,
                               TransactionCategoryRepository categoryRepository,
                               RateLimitService rateLimitService,
                               LlmInsightsClient llmInsightsClient,
                               PaymentDraftService paymentDraftService) {
        this.tools = tools;
        this.conversationRepository = conversationRepository;
        this.categoryRepository = categoryRepository;
        this.rateLimitService = rateLimitService;
        this.llmInsightsClient = llmInsightsClient;
        this.paymentDraftService = paymentDraftService;
    }

    @Transactional
    public ChatResponse chat(Long userId, String message) {
        rateLimitService.checkInsightsChat(userId);
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            return saveAndReply(userId, trimmed, ChatResponse.text("Ask about spending, budgets, or recent payments."));
        }
        log.info("Insights chat userId={} messageLength={}", userId, trimmed.length());

        Matcher draftMatcher = DRAFT_PATTERN.matcher(trimmed.toLowerCase(Locale.ROOT));
        if (draftMatcher.find()) {
            BigDecimal amount = new BigDecimal(draftMatcher.group(1));
            String handle = draftMatcher.group(2).toLowerCase(Locale.ROOT);
            PaymentDraftResponse draft = paymentDraftService.create(userId, handle, amount, "Assistant draft", "ASSISTANT");
            String reply = String.format(
                    "I prepared a payment of ₹%s to %s. Open Payment drafts or confirm with your transfer PIN (draft #%d).",
                    draft.amount(), draft.recipientHandle(), draft.id());
            return saveAndReply(userId, trimmed,
                    ChatResponse.withDraft(reply, draft.id(), draft.recipientHandle() + " ₹" + draft.amount()));
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);
        String context = buildLedgerContext(userId, lower);
        if (llmInsightsClient.isEnabled()) {
            String llm = llmInsightsClient.complete(trimmed, context);
            if (llm != null && !llm.isBlank()) {
                String reply = llm.length() > MAX_REPLY_LEN ? llm.substring(0, MAX_REPLY_LEN) + "…" : llm;
                return saveAndReply(userId, trimmed, ChatResponse.text(reply));
            }
        }

        String reply = ruleBasedReply(userId, trimmed, lower);
        if (reply.length() > MAX_REPLY_LEN) {
            reply = reply.substring(0, MAX_REPLY_LEN) + "…";
        }
        return saveAndReply(userId, trimmed, ChatResponse.text(reply));
    }

    private String ruleBasedReply(Long userId, String trimmed, String lower) {
        if (isGreetingOrHelp(lower)) {
            return APP_OVERVIEW_REPLY;
        }
        if (isAppQuestion(lower)) {
            return APP_OVERVIEW_REPLY;
        }
        if (lower.contains("what can you") || lower.contains("what do you do") || lower.contains("help me")) {
            return CAPABILITIES_REPLY;
        }
        if (lower.contains("why") && (lower.contains("fail") || lower.contains("failed"))
                && (lower.contains("payment") || lower.contains("transfer") || lower.contains("transaction"))) {
            Matcher m = Pattern.compile("DRIP[A-Z0-9]+", Pattern.CASE_INSENSITIVE).matcher(trimmed);
            if (m.find()) {
                return tools.explainFailedPayment(userId, m.group().toUpperCase(Locale.ROOT))
                        .orElse("I could not find that payment reference on your account.");
            }
            return "Share your payment reference (e.g. DRIP...) so I can look up status.";
        }
        if (lower.contains("food")
                || (lower.contains("spend") && (lower.contains("month") || lower.contains("this month")))
                || (lower.contains("how much") && lower.contains("spend"))) {
            String code = resolveCategoryFromMessage(lower);
            BigDecimal spent = tools.getMonthlySpending(userId, code);
            String label = code != null ? categoryRepository.findByCode(code).map(c -> c.getLabel()).orElse(code) : "all categories";
            return String.format("This month you've spent ₹%s on %s (from your ledger).", spent.setScale(2), label);
        }
        if (lower.contains("budget")) {
            List<Map<String, Object>> statuses = tools.getBudgetStatus(userId);
            if (statuses.isEmpty()) {
                return "You have no budgets set this month. Create one under Budgets.";
            }
            Map<String, Object> first = statuses.get(0);
            return String.format("%s budget: ₹%s of ₹%s used (%s%%).",
                    first.get("label"), first.get("spent"), first.get("limit"), first.get("percentUsed"));
        }
        if (lower.contains("compare") || lower.contains("last month")) {
            Map<String, BigDecimal> cmp = tools.compareSpending(userId);
            return String.format("Outgoing transfers: ₹%s this month vs ₹%s last month.",
                    cmp.get("thisMonth").setScale(2), cmp.get("lastMonth").setScale(2));
        }
        if (lower.contains("recent") || lower.contains("transaction")) {
            List<Map<String, Object>> recent = tools.listRecentTransactions(userId, 5);
            return recent.isEmpty() ? "No recent completed transactions." : "Recent: " + recent.toString();
        }
        if (lower.contains("category") || lower.contains("breakdown")) {
            return "Top categories: " + tools.getCategoryBreakdown(userId).toString();
        }
        return "I didn't match that to your ledger. " + CAPABILITIES_REPLY;
    }

    private String buildLedgerContext(Long userId, String lower) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("monthly_spending_all=").append(tools.getMonthlySpending(userId, null)).append('\n');
            sb.append("budgets=").append(tools.getBudgetStatus(userId)).append('\n');
            sb.append("compare=").append(tools.compareSpending(userId)).append('\n');
            sb.append("recent_tx=").append(tools.listRecentTransactions(userId, 8)).append('\n');
            sb.append("categories=").append(tools.getCategoryBreakdown(userId)).append('\n');
        } catch (Exception e) {
            sb.append("error=").append(e.getMessage());
        }
        return sb.toString();
    }

    private static boolean isGreetingOrHelp(String lower) {
        return lower.matches("^(hi|hello|hey|hii|hola)[!.?\\s]*$")
                || lower.contains("good morning") || lower.contains("good evening");
    }

    private static boolean isAppQuestion(String lower) {
        return lower.contains("about the app") || lower.contains("about app") || lower.contains("what is drippay")
                || lower.contains("what is drip pay") || lower.contains("tell me about the app")
                || lower.contains("only let me know about app") || lower.contains("know about app")
                || (lower.contains("app") && (lower.contains("what") || lower.contains("explain") || lower.contains("tell")));
    }

    private String resolveCategoryFromMessage(String lower) {
        if (lower.contains("food")) {
            return categoryRepository.findByCode("FOOD").map(c -> "FOOD").orElse(null);
        }
        return null;
    }

    private ChatResponse saveAndReply(Long userId, String userMessage, ChatResponse response) {
        if (!userMessage.isEmpty()) {
            AgentConversation userRow = new AgentConversation();
            userRow.setUserId(userId);
            userRow.setRole("user");
            userRow.setMessage(userMessage.length() > 2000 ? userMessage.substring(0, 2000) : userMessage);
            conversationRepository.save(userRow);
        }
        AgentConversation assistantRow = new AgentConversation();
        assistantRow.setUserId(userId);
        assistantRow.setRole("assistant");
        assistantRow.setMessage(response.reply());
        conversationRepository.save(assistantRow);
        return response;
    }
}
