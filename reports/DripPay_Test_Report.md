# DripPay — Test & Validation Report

**Project:** DripPay Digital Wallet  
**Report date:** 2026-10-14 (UTC)  
**Prepared by:** Automated QA validation run (repository inspection + executed tests)  
**Scope:** `backend/`, `frontend/`, `docker-compose.yml`, Flyway migrations, Postman collection  

---

## A. Executive Summary

| Item | Result |
|------|--------|
| **Overall recommendation** | **PASS WITH LIMITATIONS** |
| Backend build (`mvn package`) | **PASS** |
| Backend unit + integration tests (`mvn test`) | **PASS** — 16/16 |
| Backend application startup | **PASS** (after defect fix) |
| MySQL connectivity | **PASS** (localhost:3306, MySQL 9.7.1) |
| HTTP E2E smoke (curl) | **PASS** |
| Frontend build / Vitest / Playwright | **NOT RUN** — Node.js/npm unavailable in execution environment |
| Critical open defects | **0** |
| Financial integrity (ledger, idempotency, concurrency) | **Verified** via MySQL-backed integration tests |

**Readiness:** Suitable as a **portfolio / learning** application with simulated funds. **Not** assessed as production payment-service ready (see limitations).

**Critical issue found and fixed during QA:** Application failed to start due to Hibernate schema validation mismatch on `budgets.month_year` (`CHAR` vs `VARCHAR`). Fixed with migration `V3__budget_month_year_varchar.sql` and aligned `V1` for new installs.

---

## B. Test Environment

| Component | Detected value |
|-----------|----------------|
| Java | OpenJDK **21.0.11** (Temurin); `pom.xml` targets **17** |
| Spring Boot | **3.3.4** |
| Maven | **3.9.9** |
| MySQL | **9.7.1** (local, `root` without password in QA env) |
| Node.js / npm | **UNKNOWN** (not on PATH in QA runner) |
| React / Vite | **18.3.1 / 5.4.8** (from `frontend/package.json`, not executed) |
| Docker | **Not available** |
| OS | macOS |

### Commands executed (with evidence)

```bash
# Backend tests (after resetting IT database)
mysql -u root -e "DROP DATABASE IF EXISTS drippay_integration_test; CREATE DATABASE drippay_integration_test;"
cd backend && mvn test
# → BUILD SUCCESS, Tests run: 16, Failures: 0

cd backend && mvn package
# → BUILD SUCCESS

# Backend runtime (QA database)
mysql -u root -e "DROP DATABASE IF EXISTS drippay_qa; CREATE DATABASE drippay_qa;"
# spring-boot:run on port 18080 with DB_PASSWORD empty, DB_USERNAME=root

BASE_URL=http://localhost:18080/api/v1 ./scripts/e2e-smoke.sh
```

**Log artifacts:** `reports/logs/maven-test-full.log`, `reports/logs/e2e-smoke.log`

---

## C. Baseline Inspection (pre-fix)

| Area | Finding |
|------|---------|
| Structure | Monorepo: `backend/` (Maven), `frontend/` (Vite React), `wallet-service/` (legacy Gradle tutorial — out of DripPay scope) |
| Migrations | Flyway `V1`, `V2` (platform wallet), **missing V3** for `month_year` type |
| Backend tests (initial) | Only **3** Mockito unit tests; **no** Spring/MySQL integration tests |
| Surefire | Integration class named `*IT` was **not** discovered (filename pattern) |
| Frontend tests | **None** configured initially |
| Startup | **FAILED** — Hibernate validate error on `budgets.month_year` |
| MySQL | Port open; default `password` in README **incorrect** for this host (empty password worked) |
| Placeholder UI | Landing page showed static ₹12,450 (marketing) — functional pages use API |

---

## D. Test Execution Summary

Denominator: **automated tests that exist in repo** + **documented manual matrix** (manual rows marked NOT RUN).

| Category | Discovered | Executed | Passed | Failed | Skipped | Blocked | Not run | Pass % (executed only) |
|----------|------------|----------|--------|--------|---------|---------|---------|-------------------------|
| Backend unit (Mockito) | 4 | 4 | 4 | 0 | 0 | 0 | 0 | 100% |
| Backend integration (MySQL + MockMvc) | 12 | 12 | 12 | 0 | 0 | 0 | 0 | 100% |
| Frontend Vitest | 1 | 0 | 0 | 0 | 0 | 1 | 0 | N/A |
| E2E HTTP smoke (curl) | 1 | 1 | 1 | 0 | 0 | 0 | 0 | 100% |
| Playwright / RTL UI | 0 | 0 | 0 | 0 | 0 | 0 | 0 | NOT IMPLEMENTED |
| Manual UI/browser matrix | 42 | 0 | 0 | 0 | 0 | 0 | 42 | N/A |
| **Totals (automated)** | **18** | **17** | **17** | **0** | **0** | **1** | **0** | **100%** |

---

## E. Detailed Test Cases (automated — evidence-backed)

| ID | Feature | Status | Evidence |
|----|---------|--------|----------|
| UT-TX-01 | Self-transfer rejected | PASS | `TransferServiceTest` |
| UT-TX-02 | Insufficient balance | PASS | `TransferServiceTest` |
| UT-SP-01 | Equal split remainder | PASS | `SplitSmartTest` |
| UT-GR-01 | Custom shares must equal total | PASS | `GroupServiceSplitTest` |
| IT-AUTH-01 | Duplicate email → 409 | PASS | `DripPayFinancialIntegrationTest` |
| IT-AUTH-02 | Invalid login → 401 | PASS | same |
| IT-WAL-01 | Demo deposit increases balance | PASS | same |
| IT-TX-01 | Transfer debits/credits correctly | PASS | same |
| IT-TX-02 | Ledger debits = credits | PASS | same |
| IT-TX-03 | Idempotent transfer (no double charge) | PASS | same |
| IT-TX-04 | Idempotency key conflict → 409 | PASS | same |
| IT-TX-05 | Insufficient balance → 400, balance unchanged | PASS | same |
| IT-TX-06 | Self-transfer → 400 | PASS | same |
| IT-SEC-01 | Transaction access control (403 for non-participant) | PASS | same |
| IT-SEC-02 | Admin alerts forbidden for USER | PASS | same |
| IT-TX-07 | Concurrent transfers — only one succeeds | PASS | same |
| E2E-01 | Register, deposit, transfer, idempotent retry, overdraft | PASS | `reports/logs/e2e-smoke.log` |
| FE-UT-01 | `formatINR` | BLOCKED | Vitest added; npm not available |

### Manual / UI matrix (NOT RUN — requires Node + browser)

Registration form validation, protected routes, dashboard charts, mobile layout, Playwright flows, expired JWT redirect, etc. — **code review** indicates implementations exist and call `/api/v1/*`; **not executed** in this run.

### NOT IMPLEMENTED (per spec vs repo)

| Item | Status |
|------|--------|
| Playwright E2E | NOT IMPLEMENTED |
| React Testing Library component suite | NOT IMPLEMENTED (1 Vitest util test only) |
| ESLint | NOT IMPLEMENTED (stub `npm run lint` script) |
| Rate limiting (FraudWatch) | NOT IMPLEMENTED |
| Ledger delete/update API | Intentionally absent (append-only) |
| JWT expiry automated test | NOT RUN |
| Testcontainers | Dependency present; Docker unavailable |

---

## F. Defect Register

| ID | Severity | Description | Root cause | Fix | Regression test | Status |
|----|----------|-------------|------------|-----|-----------------|--------|
| DEF-001 | **Critical** | Backend fails startup: Hibernate validate `budgets.month_year` CHAR vs VARCHAR | Flyway `V1` used `CHAR(7)`; JPA expects `VARCHAR` | `V3` migration + `V1` updated; fresh DBs migrate cleanly | IT-WAL-01 (context loads) | **Fixed** |
| DEF-002 | **High** | MySQL integration tests not running | Surefire default includes `*Test.java` only; class named `*IT` | Renamed to `DripPayFinancialIntegrationTest` | 12 IT cases | **Fixed** |
| DEF-003 | **Medium** | Flyway checksum error on `drippay_qa` after editing `V1` | Changed migration after apply | QA uses fresh DB drop/create; IT uses `drippay_integration_test` | Documented in README/scripts | **Mitigated** |
| DEF-004 | **Low** | Landing page static ₹ amount | Decorative marketing card | Replaced with non-numeric placeholder + label | Manual | **Fixed** |
| DEF-005 | **Low** | README default DB password | Environment-specific | Use `.env`; QA used empty password | N/A | **Open (doc)** |
| DEF-006 | **Medium** | FraudWatch `onFailedTransfer` counts FAILED tx rows | Insufficient transfers throw before persisting FAILED | Document limitation; future: audit-based counter | NOT IMPLEMENTED | **Open** |

---

## G. Financial Integrity Results

**Synthetic data only.** Credentials redacted.

### Integration test evidence (MySQL `drippay_integration_test`)

| Check | Result |
|-------|--------|
| Sender/recipient balance delta equals transfer amount | PASS (IT-TX-01) |
| Failed transfer leaves balance unchanged | PASS (IT-TX-05) |
| Ledger DEBIT total = CREDIT total per transfer | PASS (IT-TX-02) |
| Duplicate `Idempotency-Key` does not double-debit | PASS (IT-TX-03) |
| Conflicting idempotency payload → 409 | PASS (IT-TX-04) |
| Concurrent 2×₹80 from ₹100 — one success, one failure | PASS (IT-TX-07) |

### E2E smoke (live `drippay_qa` on port 18080)

| Step | Result |
|------|--------|
| Deposit ₹1000 | balance 0 → 1000 |
| Transfer ₹250 with idempotency key | HTTP 200 |
| Repeat same idempotency key | HTTP 200, balance stays **750** |
| Transfer ₹99999 | HTTP **400** |

### Ledger snapshot (post-E2E, `drippay_qa`)

```text
entry_type | sum(amount)
DEBIT      | 1250.0000
CREDIT     | 1250.0000
```

Platform wallet `DRIPPLATFORM` holds contra balance for demo deposits (by design).

---

## H. Build and Test Evidence

| Command | Exit code | Summary |
|---------|-----------|---------|
| `mvn test` | 0 | 16 tests, 0 failures |
| `mvn package` | 0 | JAR built |
| `spring-boot:run` (18080) | 0 | Started after V3 |
| `e2e-smoke.sh` | 0 | See log |

Surefire reports: `backend/target/surefire-reports/`

---

## I. Security Review (non-certification)

| Check | Result |
|-------|--------|
| Passwords stored as BCrypt | PASS (integration register/login) |
| JWT required on protected routes | PASS (IT + smoke) |
| IDOR on transactions | PASS (IT-SEC-01) |
| Admin route `ROLE_ADMIN` | PASS (IT-SEC-02) |
| Default JWT secret in YAML | **Risk** — must override via `JWT_SECRET` in real deployments |
| CORS limited to configured origins | Config present (`drippay.cors`) |
| SQL injection | JPA parameterized — low risk |
| Real banking / payments | Not enabled (simulated funds) |

---

## J. Remaining Risks and Limitations

1. **Frontend not built or tested** in this environment (no Node/npm).
2. **No Playwright** or full RTL coverage.
3. **MySQL 9.7** newer than Flyway tested matrix (warning only).
4. **wallet-service/** legacy project not validated in this cycle.
5. **FraudWatch** failed-attempt rule may under-count (DEF-006).
6. **Production readiness** not claimed — portfolio/demo scope only.

---

## K. Final Recommendation

**PASS WITH LIMITATIONS**

- All **critical** backend acceptance criteria exercised in this run **passed** after fixes.
- **Financial integrity** verified with real MySQL (not mocks-only).
- **Frontend** and **browser E2E** remain **unverified** until `npm install`, `npm test`, `npm run build`, and optional Playwright are run locally.

### Re-run QA locally

```bash
./scripts/run-backend-tests.sh
# Start backend + MySQL, then:
BASE_URL=http://localhost:8080/api/v1 ./scripts/e2e-smoke.sh
cd frontend && npm install && npm test && npm run build
```

---

*Machine-readable summary:* `reports/test-summary.json`
