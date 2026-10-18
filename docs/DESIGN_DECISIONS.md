# DripPay — Design decisions

## Product direction

**Smart wallet + merchant payment gateway.** Consumer wallet proves ledger correctness; **payment gateway (`pg`)** proves merchant integration, lifecycle, webhooks, refunds, and settlement. Full target architecture: [PAYMENT_GATEWAY_ARCHITECTURE.md](./PAYMENT_GATEWAY_ARCHITECTURE.md). Package evolution: [MODULE_MAP.md](./MODULE_MAP.md).

## Modular monolith (not microservices v1)

Single Spring Boot app with clear packages (`auth`, `wallet`, **`pg`**, `ledger`, `risk`, `merchant`, `outbox`, `ai`). Easier to test, deploy, and explain in interviews. **Outbox** table lets you add Kafka later without rewriting business logic.

## Double-entry ledger + wallet balance

`wallets.balance` is the fast read model; `ledger_entries` are append-only proof. A **reconciliation job** compares net ledger position to balance and flags drift.

## Idempotency keys

Clients send `Idempotency-Key`; server stores request hash + transaction id. Replays return the same response — critical for mobile retries (Paytm-style).

## Lock ordering

Before updating two wallets, lock **lower wallet id first** to reduce deadlocks under concurrent A↔B transfers.

## AI layer

Tools read data using `SecurityUtils.currentUser()` — never trust model-supplied user ids. No tool calls `safeTransfer`. Optional `DRIPPAY_AI_API_KEY` for LLM polish; default answers are **SQL-grounded** templates.

## Webhooks

HMAC-SHA256 over payload with merchant secret. Deliveries logged in `webhook_deliveries` with retry count (publisher retries pending rows).

## Simulated funds

Demo deposits debit the platform wallet (`DRIPPLATFORM`). Not a licensed PPI or payment aggregator.
