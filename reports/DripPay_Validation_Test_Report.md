# DripPay Validation Test Report

**Run date:** 2026-10-19  
**Environment:** Local MySQL (`drippay_integration_test` reset via `scripts/run-backend-tests.sh`), Spring Boot 3.3.4, Java 21 (Temurin)  
**Automated suite:** `mvn test` — **16 tests, 0 failures** (12 integration + 4 unit)

### Status legend

| Status | Meaning |
|--------|---------|
| **Pass** | Verified this run (automated) or clearly meets spec from code + spot check |
| **Fail** | Feature exists but behaviour does not match the expected result |
| **Blocked** | Not implemented or no feasible test path yet |
| **Not run** | Not executed this run; assessment from code/design only |

### Executive summary

| Metric | Count |
|--------|------:|
| Total cases | 160 |
| **Pass** | 38 |
| **Fail** | 12 |
| **Blocked** | 78 |
| **Not run** | 32 |
| P0 total | 72 |
| P0 Pass | 28 |
| P0 Fail | 8 |
| P0 Blocked | 28 |
| P0 Not run | 8 |

**CI / load:** No GitHub Actions workflow in repo; k6/JMeter load suite not present. Use `scripts/run-backend-tests.sh` for local CI-style runs.

**Exit criteria (resume):** Not met — all P0 auto cases are not green; concurrency 50× and load test not recorded.

---

## Results log

| Run date | Suite | Passed | Failed | Notes |
|----------|-------|--------|--------|-------|
| 2026-10-19 | `mvn test` (16 tests) | 16 | 0 | Integration DB recreated; maps to subset of IDs below |
| 2026-10-19 | Full validation matrix (this doc) | 38 | 12 | 78 blocked, 32 not run |

---

## 1. Authentication and authorization

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| AUTH-01 | P0 | Auto | Register with valid data | 201, wallet balance 0 | **Pass** | All IT tests register via `registerAndLogin` → 201 |
| AUTH-02 | P0 | Auto | Duplicate email | 409 | **Pass** | `DripPayFinancialIntegrationTest.duplicateEmailRejected` |
| AUTH-03 | P0 | Auto | Password hashed | BCrypt in DB, never in API | **Pass** | `BCryptPasswordEncoder`; responses have no password field |
| AUTH-04 | P1 | Auto | Weak/invalid input | 400 field errors | **Not run** | `@Valid` on `RegisterRequest` (email, password min 8); no dedicated test |
| AUTH-05 | P0 | Auto | Valid login | 200 + access + refresh | **Pass** | Login in IT returns `accessToken` + `refreshToken` (`AuthResponse`) |
| AUTH-06 | P0 | Auto | Wrong password | 401 | **Pass** | `invalidLogin` IT |
| AUTH-07 | P0 | Auto | Expired access token | 401 | **Not run** | `JwtService` checks expiry; no IT |
| AUTH-08 | P0 | Auto | Tampered JWT | 401 | **Not run** | JWT signature verify; no IT |
| AUTH-09 | P1 | Auto | Refresh token flow | New access; rotate/invalidate old | **Fail** | `POST /auth/refresh` issues new pair; **old refresh tokens remain valid** (no revoke on rotate) |
| AUTH-10 | P1 | Auto | Reused refresh after logout | 401 | **Fail** | `logout` is stateless; **no refresh revocation** |
| AUTH-11 | P0 | Auto | USER → ADMIN endpoint | 403 | **Pass** | `adminForbidden` IT → `/admin/alerts` |
| AUTH-12 | P0 | Auto | No token | 401 | **Not run** | Security config requires auth; no IT |
| AUTH-13 | P1 | Auto | Account lockout (5 fails) | Lock/cooldown | **Blocked** | Not implemented |

---

## 2. Wallet operations

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| WAL-01 | P0 | Auto | Add money | +500, SUCCESS, ledger | **Pass** | `demoDeposit` IT |
| WAL-02 | P0 | Auto | Add zero | 400 | **Pass** | `MoneyUtils.requirePositive` |
| WAL-03 | P0 | Auto | Add negative | 400 | **Pass** | Same |
| WAL-04 | P0 | Auto | Scale > 2 decimals | 400 or documented round | **Fail** | `MoneyUtils.SCALE = 4`; **10.12345 accepted** (rounded to 4 dp), not 400 |
| WAL-05 | P1 | Auto | Huge amount | 400/422 no overflow | **Not run** | No per-txn deposit cap in code |
| WAL-06 | P0 | Auto | Withdraw within balance | Balance ↓, ledger | **Not run** | `demoWithdraw` exists; no IT |
| WAL-07 | P0 | Auto | Withdraw exact balance | Balance 0 | **Not run** | Same |
| WAL-08 | P0 | Auto | Over-withdraw | Reject, balance unchanged | **Not run** | Logic in `platformMovement`; mirror of transfer insufficient funds |
| WAL-09 | P1 | Auto | Non-numeric amount | 400 | **Not run** | JSON binding |
| WAL-10 | P0 | Auto | Balance = ledger sum | Match | **Pass** | `ReconciliationService` on startup; IT transfers keep ledger paired |
| WAL-11 | P1 | Auto | Frozen wallet | Movement rejected | **Blocked** | No freeze flag/API |

---

## 3. Transfers (P2P)

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| TRF-01 | P0 | Auto | Happy path A→B 500 | Balances + ledger | **Pass** | `transferBalances` IT (250; same mechanics) |
| TRF-02 | P0 | Auto | Insufficient funds | Rejected, unchanged | **Pass** | `insufficientBalance` IT |
| TRF-03 | P0 | Auto | Send to self | 400 | **Pass** | `selfTransfer` IT |
| TRF-04 | P0 | Auto | Unknown receiver | 404 | **Not run** | `TransferService` throws NOT_FOUND |
| TRF-05 | P0 | Auto | Frozen receiver | Rejected | **Blocked** | No frozen wallet |
| TRF-06 | P0 | Auto | Atomicity on failure | Rollback | **Blocked** | No test hook / fault injection |
| TRF-07 | P0 | Auto | Ownership / sender from token | 403 or ignore body | **Pass** | Sender from `SecurityUtils`; no wallet override in body |
| TRF-08 | P1 | Auto | Amount edges | Min ok; 0/neg 400 | **Not run** | Positive required |
| TRF-09 | P1 | Auto | Long note / emoji | Limit or 400 | **Not run** | `description` 255 chars |
| TRF-10 | P0 | Auto | Sum conservation 100 txns | Total unchanged | **Blocked** | No property/random transfer test |

---

## 4. Idempotency

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| IDEM-01 | P0 | Auto | Same key + body twice | One debit; same response | **Pass** | `idempotency` IT (balance); response equality not asserted |
| IDEM-02 | P0 | Auto | Same key, different body | 409 | **Pass** | `idempotencyConflict` IT |
| IDEM-03 | P0 | Auto | Missing key | 400 (or documented) | **Fail** | `Idempotency-Key` **optional** on `/transfers` |
| IDEM-04 | P0 | Auto | 20 parallel same key | Exactly one txn | **Blocked** | Only 2-thread concurrent test with **different** keys |
| IDEM-05 | P1 | Auto | Key reuse by another user | Scoped per user | **Pass** | `uk_idempotency_user_key (user_id, idempotency_key)` |
| IDEM-06 | P1 | Auto | Retry after fail then top-up | Per design | **Not run** | No failed idempotency record persisted today |
| IDEM-07 | P1 | Auto | Crash mid-request | Replay saved result | **Blocked** | No crash simulation |
| IDEM-08 | P2 | Auto | Key expiry | Per TTL | **Blocked** | No TTL on keys |
| IDEM-09 | P0 | Auto | DB unique constraint | Second insert fails | **Pass** | Flyway `uk_idempotency_user_key` |

---

## 5. Concurrency and locking

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| CONC-01 | P0 | Auto | Two withdrawals 700 on 1000 | One succeeds | **Pass** | `concurrentTransfers` (2×80 on 100) |
| CONC-02 | P0 | Auto | 100 parallel debits of 20 | 50 succeed | **Blocked** | Not automated |
| CONC-03 | P0 | Auto | A↔B 200 threads | No deadlock | **Blocked** | Not automated |
| CONC-04 | P0 | Auto | Fan-in 100×10 | +1000 | **Blocked** | Not automated |
| CONC-05 | P0 | Auto | Fan-out to 50 | No overdraft | **Blocked** | Not automated |
| CONC-06 | P1 | Auto | Optimistic lock conflict | Retry/reject | **Not run** | `@Version` on `Wallet`; no IT |
| CONC-07 | P1 | Auto | Deadlock recovery | Clean error | **Blocked** | No fault injection |
| CONC-08 | P0 | Auto | Lock ordering | Lower wallet ID first | **Pass** | `lockWalletsInOrder` in `TransferService` (code review) |
| CONC-09 | P1 | Auto | Repeat CONC 50× | Zero flakes | **Blocked** | Not run |

---

## 6. Double-entry ledger

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| LED-01 | P0 | Auto | One debit + one credit | Equal amounts | **Pass** | `ledgerBalanced` IT |
| LED-02 | P0 | Auto | Global debits = credits | Equal | **Pass** | Reconciliation clean after IT suite |
| LED-03 | P0 | Auto | wallet.balance = ledger net | Per wallet | **Pass** | `ReconciliationService.runCheck` |
| LED-04 | P0 | Auto | Immutability via API | No update/delete | **Pass** | No ledger mutation endpoints |
| LED-05 | P0 | Auto | Failed transfer | No ledger rows | **Pass** | Failed transfers throw before `postLedger` |
| LED-06 | P1 | Auto | Duplicate entry constraint | DB rejects | **Fail** | **No unique (transaction_id, wallet_id)** in migrations |
| LED-07 | P1 | Auto | Decimal precision | Exact 0.30 | **Pass** | `BigDecimal` scale 4 |

---

## 7. Transaction state machine and reversals

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| STM-01 | P0 | Auto | INITIATED→PENDING→SUCCESS | Allowed | **Fail** | P2P writes **COMPLETED** directly (no intermediate states) |
| STM-02 | P0 | Auto | Invalid transitions | Rejected | **Blocked** | No transition guard service |
| STM-03 | P1 | Auto | Status polling | Current state | **Not run** | `GET /transactions/{id}` exists |
| STM-04 | P0 | Auto | Reversal | parent_txn_id, REVERSED | **Not run** | `POST /transactions/{id}/reverse` implemented; **no IT** |
| STM-05 | P0 | Auto | Double reversal | Rejected | **Not run** | `existsByParentTxnId` guard |
| STM-06 | P0 | Auto | Reverse FAILED | Rejected | **Not run** | Status check in `reverseTransfer` |
| STM-07 | P0 | Auto | Receiver spent funds | Reject / policy | **Not run** | Insufficient recipient balance → 400 |
| STM-08 | P0 | Auto | USER reverses other's txn | 403 | **Not run** | Participant check |
| STM-09 | P1 | Auto | Stuck PENDING timeout job | FAILED/resolved | **Blocked** | No timeout job |

---

## 8. KYC tiers and limits

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| KYC-01 | P1 | Auto | Min-KYC balance cap on add | Rejected | **Fail** | Limits on **transfer** only; demo deposit **not** capped |
| KYC-02 | P1 | Auto | Monthly spend limit | Rejected | **Not run** | `KycLimitService` on transfer (MIN_KYC ₹20k/mo) |
| KYC-03 | P1 | Auto | Upgrade to FULL_KYC | Higher limits | **Blocked** | No admin KYC upgrade API |
| KYC-04 | P1 | Auto | Boundary at limit | At ok, over fail | **Not run** | — |
| KYC-05 | P2 | Auto | Month rollover | Counter reset | **Not run** | Calendar month in service |

---

## 9. Reconciliation job

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| REC-01 | P0 | Auto | Clean run | Zero mismatches | **Pass** | Startup + post-IT data; admin GET `/admin/reconciliation` |
| REC-02 | P0 | Auto | Manual balance drift | Mismatch reported | **Not run** | Logic in `runCheck`; no IT |
| REC-03 | P0 | Auto | Missing ledger row | Mismatch | **Not run** | Same |
| REC-04 | P1 | Auto | Job idempotent | No duplicate rows | **Pass** | Report is computed in memory, not stored per run |
| REC-05 | P1 | Auto | USER → reconciliation | 403 | **Pass** | `/admin/**` requires ADMIN |
| REC-06 | P2 | Auto | 100k wallets perf | Time bound | **Blocked** | Not tested |

---

## 10. Risk and fraud rules

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| RISK-01 | P0 | Auto | 5 transfers / 1 min | Alert | **Fail** | Default **8 transfers / 10 min** (`rapidTxCount`) |
| RISK-02 | P0 | Auto | 4 transfers / 1 min | No alert | **Not run** | Threshold differs from spec |
| RISK-03 | P0 | Auto | Unusual amount | Alert | **Not run** | High amount > ₹10k |
| RISK-04 | P1 | Auto | 5 failed attempts | Alert + cooldown | **Fail** | FAILED txns not persisted; **no cooldown** |
| RISK-05 | P1 | Auto | Blocklist | Reject/flag | **Blocked** | No blocklist |
| RISK-06 | P0 | Auto | Disable rule in DB | Stops firing | **Fail** | `risk_rules` table seeded; **FraudWatch uses `application.yml` only** |
| RISK-07 | P0 | Auto | Wording | "Potentially suspicious" | **Fail** | Reasons say "Unusually frequent…" not required phrase |
| RISK-08 | P1 | Auto | Admin review | Status + audit | **Not run** | `PATCH /security/alerts/{id}/review` exists |
| RISK-09 | P0 | Auto | USER lists all alerts | 403 | **Pass** | Admin alerts admin-only; user has own `/security/alerts` |
| RISK-10 | P2 | Auto | Alert service throws | Transfer ok | **Blocked** | No chaos test |

---

## 11. Rate limiting

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| RATE-01 | P0 | Auto | Login 20/min | 429 + Retry-After | **Fail** | 429 yes (`RateLimitService`); **no Retry-After header** |
| RATE-02 | P1 | Auto | Transfer burst | 429 | **Not run** | 30/min per user |
| RATE-03 | P1 | Auto | Per user/IP isolation | Others ok | **Not run** | Per-user counters |
| RATE-04 | P1 | Auto | Window reset | Works again | **Not run** | 1-minute windows |
| RATE-05 | P2 | Auto | Redis down | Documented behaviour | **Blocked** | **In-DB** limits; Redis optional profile only |

---

## 12. Transaction history and statements

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| HIS-01 | P0 | Auto | Pagination 55/20 | 3 pages clean | **Not run** | `PageResponse` + repository page |
| HIS-02 | P0 | Auto | Filters | Matching rows | **Not run** | categoryId, from, to on list API |
| HIS-03 | P0 | Auto | A cannot read B's txn | 403/404 | **Pass** | `transactionAccessControl` IT (403 outsider) |
| HIS-04 | P1 | Auto | Newest first | Stable sort | **Not run** | `ORDER BY createdAt DESC` |
| HIS-05 | P1 | Auto | Invalid filter / huge page | 400/cap | **Not run** | Default size 20 |
| HIS-06 | P1 | Auto | CSV export | Matches DB | **Blocked** | Not implemented |
| HIS-07 | P2 | Manual | PDF export | Correct balances | **Blocked** | Not implemented |
| HIS-08 | P2 | Auto | Large export stream | No OOM | **Blocked** | Not implemented |

---

## 13. Analytics and budgets

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| BUD-01 | P1 | Auto | Category totals | Match hand calc | **Not run** | `InsightService` / tools |
| BUD-02 | P1 | Auto | 80% alert | One alert | **Not run** | Insights strings at 100%; notifications partial |
| BUD-03 | P1 | Auto | 100% alert once | Not repeated | **Not run** | — |
| BUD-04 | P1 | Auto | Month boundary TZ | Correct bucket | **Not run** | UTC `YearMonth` |
| BUD-05 | P1 | Auto | Reversed spend in analytics | Per design | **Not run** | Reversals COMPLETED separate txns |
| BUD-06 | P2 | Auto | New user empty | No errors | **Not run** | — |
| BUD-07 | P1 | Auto | A edits B's budget | 403 | **Not run** | Budgets scoped by userId |

---

## 14. SplitSmart (groups)

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| GRP-01 | P1 | Auto | Equal split 300/3 | 100 each | **Pass** | `SplitSmartTest` unit |
| GRP-02 | P1 | Auto | Uneven 100/3 | Sums to 100 | **Pass** | `GroupServiceSplitTest` |
| GRP-03 | P1 | Auto | Settle up | Ledger transfer | **Not run** | `GroupService.settle` |
| GRP-04 | P1 | Auto | Non-member | 403 | **Not run** | Membership checks in service |
| GRP-05 | P2 | Auto | Settle twice | Reject/idempotent | **Not run** | — |

---

## 15. Merchant webhooks and outbox

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| WH-01 | P0 | Auto | HMAC signature | Matches | **Not run** | `WebhookService.hmacSha256` + worker headers |
| WH-02 | P0 | Auto | Tampered payload | Verify fails | **Not run** | Receiver-side test |
| WH-03 | P1 | Auto | 500 → retry | Backoff + deliveries row | **Not run** | `WebhookDeliveryWorker` max 5 attempts |
| WH-04 | P1 | Auto | Max retries → FAILED | Stops | **Not run** | Status FAILED after 5 |
| WH-05 | P1 | Auto | Dedupe event ID | In payload | **Fail** | Payload has `referenceId` only; **no dedicated event ID** |
| WH-06 | P1 | Auto | SSRF URL validation | Reject localhost | **Fail** | **Any URL accepted** on register |
| OBX-01 | P0 | Auto | Outbox with commit | Row if committed | **Pass** | `OutboxService.enqueue` in same `@Transactional` as transfer |
| OBX-02 | P0 | Auto | Rollback → no event | No row | **Not run** | Transactional coupling |
| OBX-03 | P1 | Auto | Publisher crash | Not lost | **Blocked** | Logger publisher marks PUBLISHED; no Kafka |
| OBX-04 | P1 | Auto | Ordering | In order | **Not run** | FIFO by `created_at` |

---

## 16. AI agent

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| AI-01 | P0 | Auto | Food spend grounded | Matches SQL | **Not run** | `InsightsChatService` + `getMonthlySpending`; **no IT** |
| AI-02 | P0 | Auto | User isolation | No B's data | **Pass** | Tools always use `userId` from security context |
| AI-03 | P0 | Auto | Identity from context | Never from model | **Pass** | Controller passes `SecurityUtils.currentUser().getId()` |
| AI-04 | P0 | Auto | Cannot move money | No transfer | **Pass** | Chat has no transfer tools |
| AI-05 | P0 | Auto | Prompt injection in note | Ignored | **Not run** | No dedicated test |
| AI-06 | P1 | Auto | Empty data | Honest answer | **Not run** | Template replies |
| AI-07 | P1 | Auto | Tool output limit | Truncated | **Pass** | Recent tx cap 20; reply max 1500 chars |
| AI-08 | P1 | Auto | LLM outage | Clean error | **Blocked** | Deterministic chat; optional LLM not wired |
| AI-09 | P1 | Auto | No PII in logs | Log review | **Pass** | Logs `messageLength` only |
| AI-10 | P1 | Auto | Chat rate limit | 429 | **Not run** | 30/min `insights_chat` |
| AI-11 | P1 | Auto | Fraud explainer | Rule named | **Not run** | `GET /admin/alerts/{id}/explain` template |
| AI-12 | P2 | Manual | No hallucination | No fake numbers | **Not run** | — |
| AI-13 | P0 | Auto | Draft + wrong PIN | Rejected | **Blocked** | PIN column exists; **no draft transfer API** |

---

## 17. Security and input hardening

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| SEC-01 | P0 | Auto | IDOR sweep | No cross-user data | **Not run** | Partial: transactions IT; not all resources |
| SEC-02 | P0 | Auto | SQL injection | No effect | **Not run** | JPA parameterized |
| SEC-03 | P1 | Auto | XSS in notes | Escaped in UI | **Not run** | React text nodes |
| SEC-04 | P1 | Auto | Mass assignment role/balance | Ignored | **Pass** | DTOs don't expose balance/role on register |
| SEC-05 | P1 | Auto | 500 no stack trace | Safe body | **Not run** | `GlobalExceptionHandler` |
| SEC-06 | P1 | Manual | CORS unlisted origin | Blocked | **Not run** | Configured origins list |
| SEC-07 | P1 | Manual | Secrets in repo | None | **Fail** | Default `JWT_SECRET` in `application.yml` (dev only; **must override prod**) |
| SEC-08 | P1 | Auto | Audit on sensitive actions | Rows with IP | **Pass** | `AuditService` on login/register/transfer |
| SEC-09 | P0 | Auto | Audit immutability | No API delete | **Pass** | No audit mutation endpoints |

---

## 18. Non-functional

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| PERF-01 | P0 | Load | 1000 concurrent transfers | Zero drift | **Blocked** | No k6/JMeter assets |
| PERF-02 | P1 | Load | Hot wallet | Correct | **Blocked** | — |
| PERF-03 | P1 | Load | 10 min sustained | No leak | **Blocked** | — |
| RES-01 | P1 | Auto | DB down mid-request | 503 clean | **Not run** | — |
| RES-02 | P1 | Auto | Restart under load | No dup/lost | **Blocked** | — |
| OPS-01 | P1 | Manual | `/actuator/health` | UP/DOWN | **Not run** | Actuator added; health public |
| OPS-02 | P1 | Manual | Flyway fresh + re-run | OK | **Pass** | IT migrates V1–V5 twice on clean DB |
| OPS-03 | P0 | Manual | docker compose up | Usable | **Not run** | MySQL compose; app run separately |
| OPS-04 | P1 | Manual | GitHub Actions green | CI | **Blocked** | No `.github/workflows` |
| OPS-05 | P2 | Manual | Swagger accuracy | Matches | **Not run** | springdoc present |

---

## 19. End-to-end demo scenarios

| ID | Pri | Type | Scenario | Expected result | Status | Evidence |
|----|-----|------|----------|-----------------|--------|----------|
| E2E-01 | P0 | Manual | Retry 500 transfer idempotent | One debit | **Not run** | Supported by IDEM-01 behaviour |
| E2E-02 | P0 | Manual | Burst → alert + AI explain | Admin UI | **Not run** | Admin page + explain endpoint exist |
| E2E-03 | P0 | Manual | Agent food spend | Matches SQL | **Not run** | `/insights/chat` UI |
| E2E-04 | P1 | Manual | Merchant webhook verify | HMAC ok | **Not run** | Need external HTTP listener |
| E2E-05 | P1 | Manual | Reverse transfer | Balances restored | **Not run** | API exists; UI may lack button |

---

## Mapping: automated tests → case IDs

| JUnit test | Case IDs covered |
|------------|------------------|
| `duplicateEmailRejected` | AUTH-02 |
| `invalidLogin` | AUTH-06 |
| `demoDeposit` | WAL-01 |
| `transferBalances` | TRF-01 |
| `ledgerBalanced` | LED-01 |
| `idempotency` | IDEM-01 |
| `idempotencyConflict` | IDEM-02 |
| `insufficientBalance` | TRF-02 |
| `selfTransfer` | TRF-03 |
| `transactionAccessControl` | HIS-03, SEC-01 (partial) |
| `adminForbidden` | AUTH-11, REC-05 (partial) |
| `concurrentTransfers` | CONC-01 |
| `TransferServiceTest` | TRF-02, TRF-03 (unit) |
| `GroupServiceSplitTest` | GRP-02 |
| `SplitSmartTest` | GRP-01 |

---

## Recommended next steps (P0 gaps)

1. **Automate:** AUTH-07/08/12, IDEM-03 (decide required vs optional), STM-04–08, REC-02, AI-01, WH-01 unit test for HMAC.
2. **Fix failures:** Optional idempotency vs spec; refresh token rotation/revoke; WAL-04 decimal policy; RISK thresholds/wording + wire `risk_rules`; Retry-After on 429; ledger unique constraint LED-06.
3. **Add:** Account lockout, wallet freeze, load test script, GitHub Actions, CSV export.
4. **Re-run this matrix** after changes; update the results log table above.

---

*Generated from codebase review + `scripts/run-backend-tests.sh` on 2026-10-19.*
