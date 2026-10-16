# DripPay — QA & test documentation

This folder holds **validation reports**, **manual test cases**, **machine-readable summaries**, and **execution logs** for the DripPay app (`backend/` + `frontend/`).

> **Scope:** DripPay only. The separate Gradle project `wallet-service/` is not covered here.

---

## Documents

| File | Purpose |
|------|---------|
| [**DripPay_Manual_Test_Cases.md**](./DripPay_Manual_Test_Cases.md) | **Manual / UI** checklist (~**186** cases): E2E journeys, per-screen flows, API edge cases, financial checks |
| [**DripPay_Test_Report.md**](./DripPay_Test_Report.md) | Formal **QA report**: environment, automated test results, defects, financial integrity, security notes, recommendation |
| [**test-summary.json**](./test-summary.json) | **JSON summary** of the same QA run (status counts, defects, integrity flags) |
| **`logs/`** | Captured output from Maven tests, backend startup, and `e2e-smoke.sh` |

---

## Before manual testing

1. **MySQL** running locally (`drippay` database; empty `root` password is common on dev machines).
2. **Backend**
   ```bash
   cd backend
   export SERVER_PORT=18080   # optional, if 8080 is in use
   ./mvnw spring-boot:run
   ```
3. **Frontend**
   ```bash
   cd frontend
   npm install
   # frontend/.env
   # VITE_DEV_API_PROXY=http://localhost:18080
   npm run dev
   ```
4. Open **http://localhost:5173** and use seeded accounts from the manual test doc (or register new users).

---

## Automated tests (run from repo root)

| Command | What it checks |
|---------|----------------|
| `./scripts/run-backend-tests.sh` | Resets IT DB, runs `mvn test` (unit + MySQL integration) |
| `cd backend && ./mvnw test` | Same tests without the helper script |
| `BASE_URL=http://localhost:18080/api/v1 ./scripts/e2e-smoke.sh` | Live API: register, deposit, transfer, idempotency, overdraft |
| `cd frontend && npm test` | Vitest (utility tests; extend as needed) |

Point `BASE_URL` at your backend port (`8080` or `18080`).

---

## How to use the manual test cases

1. Open [**DripPay_Manual_Test_Cases.md**](./DripPay_Manual_Test_Cases.md).
2. Follow the **Suggested test order (1-hour walkthrough)** for a first pass.
3. Mark each case **Pass / Fail / Blocked** in your own sheet or issue tracker.
4. For failures, note **browser**, **API port**, and **HTTP status** from the network tab.

**Traceability:** The manual doc maps many cases to automated IDs (e.g. `IT-TX-03`, `E2E-01`) listed in the test report.

---

## Latest QA snapshot (see JSON for details)

- **Overall:** `PASS_WITH_LIMITATIONS` (backend + financial integrity strong; full UI E2E not automated in repo).
- **Backend automated:** 16/16 passed when MySQL is available.
- **Frontend build/UI:** run `npm run build` and manual cases locally to refresh status.

Regenerate logs by re-running the scripts above; update `DripPay_Test_Report.md` after major releases if you keep a formal report.

---

## Related

- Project overview & run instructions: [../README.md](../README.md)
- API collection: [../postman/DripPay.postman_collection.json](../postman/DripPay.postman_collection.json)

**Disclaimer:** DripPay uses **simulated funds only** — not a regulated payment service.
