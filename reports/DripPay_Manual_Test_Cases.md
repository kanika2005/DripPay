# DripPay — Manual & UI Test Cases

See **[reports/README.md](./README.md)** for how to run automated tests, environment setup, and other QA artifacts.

**App:** React SPA (`frontend/`) + API (`backend/`)  
**Preconditions:** MySQL up, backend running (`SERVER_PORT=18080` or `8080`), frontend `npm run dev`, `VITE_DEV_API_PROXY` matches backend port.

**Demo accounts (seeder):**

| Email | Password | Role |
|-------|----------|------|
| `demo@drippay.app` | `Demo1234!` | USER |
| `admin@drippay.app` | `Admin1234!` | ADMIN |

**Automated coverage (run separately):** `./scripts/run-backend-tests.sh`, `BASE_URL=http://localhost:18080/api/v1 ./scripts/e2e-smoke.sh`

**How to record results:** Add columns `Pass | Fail | Blocked | Notes | Tester | Date` in your sheet, or copy tables into Excel/Sheets.

---

## End-to-end flows (validate full journeys)

| Flow ID | Journey | Steps (high level) | Pass criteria |
|---------|---------|-------------------|---------------|
| E2E-F01 | **New user onboarding** | Landing → Register → Dashboard welcome → Wallet deposit → Dashboard stats update | Balance &gt; 0; welcome card gone after activity; no console errors |
| E2E-F02 | **P2P happy path** | User A deposit → copy wallet ID → User B register → A sends ₹100 → B wallet + history | Both balances correct; both see tx in list/detail |
| E2E-F03 | **Spend & analyze** | Send with category FOOD → Analytics pie → Dashboard SpendSense insight | Category appears in chart; monthly spending increases |
| E2E-F04 | **Budget loop** | Set FOOD budget ₹500 → spend FOOD &gt; ₹500 → Dashboard insights | Budget row saved; insight mentions budget if rule fires |
| E2E-F05 | **SplitSmart solo** | Create group → add expense → balances show User # owes/paid | Net balances math matches equal split |
| E2E-F06 | **Fraud + admin** | Large transfer → Security page alert → Admin lists same alert | Alert reason mentions rule; admin sees cross-user list |
| E2E-F07 | **Session lifecycle** | Login → refresh browser → still authenticated → Logout → `/dashboard` blocked | `/users/me` on refresh; token cleared on logout |
| E2E-F08 | **Failure recovery** | Insufficient transfer → balance unchanged → deposit → retry succeeds | No partial debit; second attempt 200 |

---

## 1. Environment & smoke

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| ENV-01 | Backend health | Open Swagger `http://localhost:<port>/swagger-ui.html` | UI loads; `/api/v1/*` endpoints listed |
| ENV-02 | OpenAPI JSON | GET `/api/v1/docs` or swagger api-docs | 200 JSON |
| ENV-03 | Frontend proxy | Register with backend stopped, then started | Clear error when down; success when up |
| ENV-04 | API smoke script | Run `e2e-smoke.sh` | Exit 0; balance 750 after idempotent transfer |
| ENV-05 | Port mismatch | Frontend proxy 8080, backend on 18080 | Register fails or network error; fix `.env` → works |
| ENV-06 | Flyway on startup | Restart backend twice | Second start: schema up to date; no migration errors |
| ENV-07 | Demo seeder | Fresh DB + backend start | Demo + admin users exist; wallets exist for both |

---

## 2. Public pages & routing

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| PUB-01 | Landing | Visit `/` | Headline, feature bullets, Get DripPay / Demo login |
| PUB-02 | Landing CTAs | Click Log in / Get DripPay | Routes to `/login`, `/register` |
| PUB-03 | Unknown route | Visit `/foo` | Redirect to `/` |
| PUB-04 | Deep link while logged out | Visit `/transactions` | Redirect to `/login` |
| PUB-05 | Deep link while logged in | Visit `/send` directly | SafeTransfer loads inside layout |
| PUB-06 | Logo / brand | Click logo from login | Navigates per app design (home or dashboard) |

---

## 3. Registration flow

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| REG-01 | Happy path | Valid full name, email, password ≥8 | 201; auto-login; `/dashboard`; toast |
| REG-02 | Duplicate email | Same email twice | 409; error message |
| REG-03 | Invalid email | `not-an-email` | 400; field validation message |
| REG-04 | Password too short | 7 characters | Browser `minLength` or API 400 |
| REG-05 | Password boundary | Exactly 8 characters | Success |
| REG-06 | Full name too short | 1 character | API 400 (`min = 2`) |
| REG-07 | Full name max | 121+ characters | API 400 (`max = 120`) |
| REG-08 | Empty fields | Submit with blanks | HTML `required` prevents submit |
| REG-09 | Email normalization | Register `Test@Mail.com`; login `test@mail.com` | Login works if backend lowercases (verify behavior) |
| REG-10 | Wallet created | After register, GET `/wallets/me` | `publicWalletId` present; balance 0 |
| REG-11 | Link to login | From register page | Navigates to `/login` |

---

## 4. Login & session

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| AUTH-01 | Login success | Valid demo credentials | Dashboard; token in `localStorage` `drippay_token` |
| AUTH-02 | Wrong password | Bad password | 401; error text; no token |
| AUTH-03 | Unknown email | Non-existent user | 401 |
| AUTH-04 | Empty password | Submit empty | Blocked by `required` |
| AUTH-05 | Session restore | Login → hard refresh (F5) | Still on dashboard; user loaded via `/users/me` |
| AUTH-06 | Invalid token on refresh | Corrupt `drippay_token` → refresh | Token cleared; redirected to login on protected route |
| AUTH-07 | Protected route guard | Logout → `/dashboard` | Redirect `/login` |
| AUTH-08 | Logout | Logout from layout | Token & user removed; API calls 401 |
| AUTH-09 | Logout API | POST `/auth/logout` with valid JWT | 200/204 (stateless; client clears token) |
| AUTH-10 | Session expired banner | Visit `/login?expired=1` | Amber “Session expired” message |
| AUTH-11 | JWT missing on API | `curl` `/wallets/me` no header | 401 |
| AUTH-12 | Admin without wallet | User row, no wallet (edge DB) | Login must not 500; fix wallet or expect documented error |
| AUTH-13 | Login loading state | Slow network on submit | Button shows “Signing in…”; disabled |
| AUTH-14 | Register → immediate send | New user, zero balance, send money | Insufficient balance error (not crash) |

---

## 5. Dashboard flow

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| DASH-01 | Balance card | Open `/dashboard` | Matches `/wallets/me` balance |
| DASH-02 | Wallet ID shown | Compare to Wallet page | Same `publicWalletId` |
| DASH-03 | Quick actions | Send / Add demo funds / Transactions links | Correct routes |
| DASH-04 | Welcome empty state | New user, no tx, ₹0 | Welcome card visible |
| DASH-05 | Welcome hidden | After deposit or transfer | Welcome card not shown |
| DASH-06 | Monthly stats | After send/receive this month | Sent / Received / Spending figures update |
| DASH-07 | SpendSense block | Budget exceeded or insights exist | Purple insight list renders |
| DASH-08 | Recent transactions | After transfer | Up to 5 items; link to detail |
| DASH-09 | View all link | Click | Goes to `/transactions` |
| DASH-10 | Loading skeleton | Throttle network on first load | Pulse placeholders, then data |

---

## 6. Navigation & layout

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| NAV-01 | All sidebar items | Dashboard, Wallet, Send, Transactions, Analytics, Budgets, Groups, Security, Profile | Each page loads; no 404 |
| NAV-02 | Admin link (USER) | Login as demo user | No Admin entry |
| NAV-03 | Admin link (ADMIN) | Login as admin | Admin visible; `/admin` works |
| NAV-04 | USER on `/admin` | Direct URL | Redirect to `/dashboard` |
| NAV-05 | Active route styling | Click each item | Current route highlighted (if styled) |
| NAV-06 | Logout from layout | Use sidebar/logout control | Session cleared |

---

## 7. Wallet & demo funds flow

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| WAL-01 | View wallet | `/wallet` | Balance + public ID |
| WAL-02 | Demo deposit | ₹500 | Balance +500; toast; tx type DEPOSIT in history |
| WAL-03 | Demo withdrawal | ₹200 (≤ balance) | Balance −200; toast |
| WAL-04 | Over-withdraw | Amount &gt; balance | Error; balance unchanged |
| WAL-05 | Deposit zero (API) | POST `amount: 0` | 400 |
| WAL-06 | Deposit negative (API) | POST `amount: -10` | 400 |
| WAL-07 | Decimal amounts | Deposit ₹100.50 | Balance respects 2–4 decimal rules |
| WAL-08 | Multiple deposits | 3 deposits in row | Balance = sum |
| WAL-09 | Withdraw exact balance | Withdraw all | Balance 0; further withdraw fails |
| WAL-10 | Dashboard sync | Deposit on Wallet → Dashboard | Balance matches without manual refresh (re-navigate) |
| WAL-11 | Lookup own wallet (API) | GET `/wallets/lookup/{ownId}` | 400 cannot send to own wallet |
| WAL-12 | Lookup valid peer (API) | GET lookup for User B ID | `displayName` returned |
| WAL-13 | Lookup invalid ID | `DRIPNOTREAL` | 404 |
| WAL-14 | Case on wallet ID | Enter lowercase in Send lookup | UI uppercases; lookup works |

---

## 8. SafeTransfer — UI step flow

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| TX-UI-01 | Step 1 empty continue | Click Continue with empty ID | Button disabled |
| TX-UI-02 | Step 1 invalid ID | Bogus ID → Continue | Error message |
| TX-UI-03 | Step 2 back | Back button | Returns to step 1; recipient preserved |
| TX-UI-04 | Step 2 confirm disabled | No amount | Confirm disabled |
| TX-UI-05 | Full send | Valid ID → amount → note → category → Confirm | Toast; navigate to receipt |
| TX-UI-06 | Double-click confirm | Rapid double click Confirm | Single transfer (idempotency key per attempt — ideally one tx) |
| TX-UI-07 | Processing state | During submit | “Processing…”; button disabled |
| TX-UI-08 | Categories | Each option FOOD, SHOPPING, etc. | Saved on transaction detail |

---

## 9. SafeTransfer — API & business rules

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| TX-01 | Successful transfer | A → B ₹250 | A −250, B +250; status COMPLETED |
| TX-02 | Insufficient balance | Amount &gt; balance | 400; balances unchanged |
| TX-03 | Self-transfer | Own wallet ID | 400 |
| TX-04 | Amount 0.01 | Minimum transfer | Success |
| TX-05 | Amount 0 | API | 400 validation |
| TX-06 | Missing recipient | Blank wallet id | 400 |
| TX-07 | Idempotency replay | Same `Idempotency-Key` + same body twice | 200 both; one debit |
| TX-08 | Idempotency conflict | Same key, different amount | 409 |
| TX-09 | No idempotency header | POST without header | Still works; duplicate clicks may duplicate (document risk) |
| TX-10 | Note on transfer | note: "lunch" | Visible on detail `description` |
| TX-11 | Recipient receives | B checks `/wallets/me` | Balance increased |
| TX-12 | Concurrent drain (API) | Two parallel transfers from ₹100 | At most one succeeds (automated IT-TX-07) |

---

## 10. Transaction history & detail flow

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| TXL-01 | Empty list | New user | Empty state message |
| TXL-02 | Row content | After P2P | Reference, type, amount, date |
| TXL-03 | Link to detail | Click reference | `/transactions/:id` receipt |
| TXL-04 | Receipt fields | Open detail | Status, type, amount, from/to, category, note, date |
| TXL-05 | Deposit in list | After demo deposit | Type reflects deposit |
| TXL-06 | Pagination next | &gt;15 transactions | Page 2 loads via Next |
| TXL-07 | Pagination prev | From page 2 | Prev returns page 1 |
| TXL-08 | Pagination boundary | Page 1 Prev disabled | Disabled state |
| TXL-09 | Last page Next | On last page | Next disabled |
| TXL-10 | Invalid tx id (API) | GET `/transactions/999999` | 404 |
| TXL-11 | Reference lookup (API) | GET `/transactions/reference/{ref}` as participant | 200 |
| TXL-12 | Reference lookup stranger | B uses A’s reference | 403 |
| TXL-13 | List filter (API) | `categoryId`, `from`, `to` query params | Filtered page (Swagger/Postman) |

---

## 11. SpendSense / Analytics flow

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| INS-01 | Empty analytics | No outgoing categorized tx | Empty state copy |
| INS-02 | Pie chart | Send FOOD + SHOPPING | Chart segments render |
| INS-03 | Tooltip | Hover segment | INR formatted tooltip |
| INS-04 | List below chart | Same data | Labels + amounts match chart |
| INS-05 | Summary API | GET `/insights/summary` | `monthlySent`, `monthlyReceived`, `monthlySpending`, `insights[]` |
| INS-06 | Categories API | GET `/insights/categories` | Array with `categoryCode`, `label`, `amount` |
| INS-07 | Month boundary | Tx last month vs this month (if test data) | Current month only in analytics copy |
| INS-08 | TRANSFER category | Default category spend | Appears in breakdown when used |

---

## 12. Budgets flow

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| BUD-01 | Create budget | FOOD ₹3000 | Row in list with current `monthYear` |
| BUD-02 | Each category | FOOD, SHOPPING, TRANSPORT, ENTERTAINMENT, BILLS | All savable |
| BUD-03 | Update limit | Same category again | New limit shown (upsert) |
| BUD-04 | Limit below ₹1 (API) | `monthlyLimit: 0.50` | 400 (`@DecimalMin("1.00")`) |
| BUD-05 | Unknown category (API) | `categoryCode: INVALID` | 400 unknown category |
| BUD-06 | List empty | No budgets | Empty state text |
| BUD-07 | Insight linkage | Spend over budget in category | Dashboard insight mentions budget (if implemented) |

---

## 13. SplitSmart (groups) flow

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| GRP-01 | Create group | Name only (UI) | Group in list; creator is member |
| GRP-02 | Empty name | Create disabled | Button disabled |
| GRP-03 | Open group | Click group | Balances panel |
| GRP-04 | No expenses | New group | Empty or zero nets |
| GRP-05 | Add expense | Title + ₹300 | Toast “no auto wallet movement”; balances update |
| GRP-06 | Equal split 1 member | Solo group expense | Full amount net to payer logic |
| GRP-07 | Multiple expenses | 2 expenses same group | Cumulative nets |
| GRP-08 | Expense zero (API) | `totalAmount: 0` | 400 |
| GRP-09 | Blank title (API) | Empty title | 400 |
| GRP-10 | Add member (API) | POST group with `memberUserIds: [otherUserId]` | Both see group |
| GRP-11 | Two-member split | API expense ₹100, 2 members | ₹50 shares (remainder on first) |
| GRP-12 | Non-member expense (API) | User C adds expense to A’s group | 403 |
| GRP-13 | Settlement (API) | POST `/groups/{id}/settlements` with `toUserId`, amount | Wallet transfer + settlement record |
| GRP-14 | Settlement insufficient funds | Settle more than balance | 400 |
| GRP-15 | UI settlement | If not in UI | Document N/A; test via Postman only |

---

## 14. FraudWatch (user) flow

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| FRAUD-01 | High amount rule | Transfer &gt; ₹10,000 (after deposit) | New alert; reason references high amount |
| FRAUD-02 | Rapid transfers | 8+ completed transfers in 10 minutes | Rapid activity alert (threshold config) |
| FRAUD-03 | Empty security page | Clean user | “No alerts” message |
| FRAUD-04 | Alert card content | After trigger | Severity badge, timestamp, reason, review status |
| FRAUD-05 | List API | GET `/security/alerts` | Only current user’s alerts |
| FRAUD-06 | Review own alert (API) | PATCH `/security/alerts/{id}/review` | Status updated |
| FRAUD-07 | Review others’ alert | User B patches A’s alert | 403 |
| FRAUD-08 | Failed transfer count rule | Many insufficient transfers | May **not** alert (known limitation DEF-006) |

---

## 15. Admin flow

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| ADM-01 | List all alerts | `/admin` as ADMIN | All users’ alerts, newest first |
| ADM-02 | Alert content | Each row | Severity, review status, reason |
| ADM-03 | USER API | GET `/api/v1/admin/alerts` as USER | 403 |
| ADM-04 | Unauthenticated admin API | No JWT | 401 |
| ADM-05 | Empty admin | No alerts in system | Empty list (no crash) |
| ADM-06 | Cross-user visibility | Alert for demo user visible to admin | Shown in admin list |

---

## 16. Profile flow

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| PROF-01 | View profile | `/profile` | Email read-only; wallet ID; name field |
| PROF-02 | Update name | Change full name → Save | Toast; PATCH `/users/me` 200 |
| PROF-03 | Persist name | Refresh after save | Name still updated (via `/users/me` on load) |
| PROF-04 | Blank name (API) | PATCH `fullName: ""` | 400 |
| PROF-05 | Role display | ADMIN vs USER | Correct role in UI if shown |

---

## 17. Financial integrity & data

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| FIN-01 | Ledger balanced | SQL: sum debits = credits per transaction | Equal |
| FIN-02 | Global ledger | Sum all DEBIT = sum all CREDIT | Equal (platform + users) |
| FIN-03 | Demo deposit ledger | After deposit | Platform debit; user credit |
| FIN-04 | P2P ledger | After transfer | Sender debit; recipient credit |
| FIN-05 | Failed transfer | Insufficient funds | No ledger rows for failed attempt (or consistent FAILED status) |
| FIN-06 | Balance vs ledger | Wallet balance matches net of entries | Reconciliation holds |
| FIN-07 | No negative balance | DB constraint | Cannot persist negative `wallets.balance` |

---

## 18. Security & authorization (API)

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| SEC-01 | IDOR transaction | B GET A’s transaction id | 403 |
| SEC-02 | Admin only | USER → admin endpoints | 403 |
| SEC-03 | BCrypt passwords | DB `password_hash` | Not plain text |
| SEC-04 | CORS | Browser from unlisted origin | Blocked |
| SEC-05 | Swagger without auth | Try protected endpoint in Swagger Authorize | 401 without Bearer |

---

## 19. UX, accessibility & resilience

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| UX-01 | Toasts | Success/error actions | Toast shows and clears |
| UX-02 | INR formatting | All money displays | `formatINR` style consistent |
| UX-03 | Mobile 375px | Resize viewport | Usable layout; sidebar behavior acceptable |
| UX-04 | Tablet 768px | Resize | Grid layouts adapt |
| UX-05 | Loading states | Throttle 3G | Skeletons on dashboard/wallet/transactions |
| UX-06 | Error on detail | Invalid `/transactions/abc` | Graceful error or loading stop |
| UX-07 | Keyboard submit | Login/register Enter key | Submits form |
| UX-08 | Focus visible | Tab through login fields | Focus ring visible |

---

## 20. Postman / API regression (optional)

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| API-01 | Import collection | `postman/DripPay.postman_collection.json` | All folders present |
| API-02 | Auth folder | Register + login requests | Tokens flow to other requests |
| API-03 | Transfer folder | Transfer with idempotency | 200 + balance change |
| API-04 | Groups folder | Create + expense + settle | Documented responses |

---

## 21. Negative & edge cases

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| NEG-01 | Malformed JSON | POST transfer body invalid | 400 |
| NEG-02 | Expired JWT | Use very old token (if you shorten expiry in test) | 401 |
| NEG-03 | SQL injection string | Email `'; DROP TABLE users;--` | Safe rejection; no DB harm |
| NEG-04 | XSS in note | Note with `<script>` | Stored escaped/safe in UI |
| NEG-05 | Huge note | Very long note string | Validation or truncate per API |
| NEG-06 | Unicode name | Register full name with emoji | Accept or validate gracefully |

---

## Suggested test orders

### Quick smoke (~20 min)

ENV-01 → AUTH-01 → WAL-02 → TX-UI-05 → TXL-03 → AUTH-08

### Full regression (~3–4 hours)

Run all sections 1–19 in order; log Pass/Fail; file bugs with ID reference.

### Pre-demo checklist (~45 min)

E2E-F01, E2E-F02, E2E-F06, DASH-01, INS-02, BUD-01, GRP-05, ADM-01

---

## Traceability to automated tests

| Manual IDs | Automated |
|------------|-----------|
| REG-02, AUTH-02 | IT-AUTH-01, IT-AUTH-02 |
| WAL-02, TX-01–08, FIN-* | IT-WAL-01, IT-TX-01–07, E2E-01 |
| SEC-01, ADM-03 | IT-SEC-01, IT-SEC-02 |
| GRP-11 | UT-SP-01, UT-GR-01 |
| TX-12 | IT-TX-07 |

---

## Case count summary

| Section | Approx. cases |
|---------|----------------|
| E2E flows | 8 |
| Environment | 7 |
| Public / routing | 6 |
| Registration | 11 |
| Login / session | 14 |
| Dashboard | 10 |
| Navigation | 6 |
| Wallet | 14 |
| SafeTransfer UI | 8 |
| SafeTransfer API | 12 |
| Transactions | 13 |
| Analytics | 8 |
| Budgets | 7 |
| Groups | 15 |
| FraudWatch user | 8 |
| Admin | 6 |
| Profile | 5 |
| Financial integrity | 7 |
| Security API | 5 |
| UX | 8 |
| Postman | 4 |
| Negative / edge | 6 |
| **Total** | **~186** |
