# DripPay Roadmap

Backend-first wallet & transaction intelligence platform. **Only claim on your resume what is built and tested.**

See [docs/DESIGN_DECISIONS.md](docs/DESIGN_DECISIONS.md) for architecture rationale.

## Guiding rules

- Modular **monolith**; Kafka is a later upgrade (outbox table is Kafka-ready).
- Money is `DECIMAL(19,4)` — never float.
- **AI is read-only** — never moves money; drafts need user PIN confirm (when implemented).
- Redis is optional (rate limits use in-DB sliding window by default).

## Status legend

- [x] Done in repo
- [~] Partial
- [ ] Not started

---

## Week 1–2: Core & money engine

- [x] Spring Boot 3, Java 17, MySQL, Flyway
- [~] Package modules (`com.drippay.*` — logical modules, single deployable)
- [x] Register / login, BCrypt, JWT
- [~] JWT refresh tokens (V5 + `/auth/refresh`)
- [x] Roles USER, ADMIN — [~] MERCHANT enum added
- [x] Auto wallet on register
- [x] Demo deposit, withdraw, P2P transfer
- [x] Global exception handler
- [x] Swagger `/swagger-ui.html`
- [x] Double-entry ledger
- [x] Idempotency-Key + `idempotency_records`
- [x] Pessimistic locks + lock ordering
- [x] `@Version` on wallets
- [~] State machine: PENDING → COMPLETED (+ INITIATED/REVERSED in V5)
- [x] Transaction status via GET `/transactions/{id}`
- [x] History + pagination
- [x] Integration tests (MySQL)

## Week 3: Trust & safety

- [~] Reversal flow (`POST /transactions/{id}/reverse`)
- [x] Reconciliation job + admin report
- [x] Audit logs
- [~] KYC tiers (MIN_KYC / FULL_KYC columns + limit service)
- [x] Rule-based FraudWatch
- [~] `risk_rules` table + feature toggles
- [~] In-DB rate limiting (login + transfer)
- [x] Admin alerts

## Week 4: Differentiators

- [x] Outbox table + scheduled publisher
- [x] Merchant webhooks (HMAC-SHA256, deliveries + retries)
- [x] Collect / QR `name@dripay`
- [x] Razorpay test mode (simulated default; live when `RAZORPAY_KEY_*` set)
- [x] Categories, budgets, insights
- [x] SplitSmart groups
- [x] CSV/PDF statements (`GET /api/v1/statements/export`)

## Week 5: AI (assistive)

- [x] Insights chat API with read-only tools + optional LLM (`DRIPPAY_AI_API_KEY`)
- [~] Fraud explainer (template-based; LLM optional via env)
- [~] Support: payment failure lookup via chat tools
- [x] Payment draft + PIN confirm (`/payment-drafts`, Assistant draft phrases)

## UI theme

Dark-first trust-focused fintech palette — see [docs/UI_THEME.md](docs/UI_THEME.md).

## Week 6: Polish

- [x] React dashboard (core flows)
- [x] Notifications + SplitSmart member UX
- [~] Actuator health
- [~] Docker Compose (+ Redis optional)
- [x] CI script `run-backend-tests.sh`
- [~] k6 load test + recorded numbers (`load/k6/transfers.js`; fill `load/RESULTS.md`)
- [x] README + this roadmap
- [x] Design decisions doc

## Demo script

1. Idempotent transfer retry — single debit  
2. Reconciliation — zero mismatches  
3. Rapid transfers → alert → admin / AI explain  
4. Insights chat — food spend matches SQL  
5. Webhook — valid HMAC signature  

## Resume

Use bullets only for **[x]** items above; add measured concurrency numbers after load tests.
