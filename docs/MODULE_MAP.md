# DripPay — Backend module map

Single deployable: `backend/` → `com.drippay`.

## Current (implemented)

```
com.drippay
├── auth / security          JWT, refresh tokens
├── controller.*             REST (wallet, transfers, groups, …)
├── collect                  name@drippay handles
├── payment                  drafts, Razorpay simulate
├── statement                CSV/PDF export
├── merchant                 webhook endpoints + deliveries (user-scoped today)
├── outbox                   transactional events + publisher
├── reconciliation           balance vs ledger
├── ai                       user insights chat + tools
├── risk / ratelimit / kyc
└── service.*                TransferService (core ledger posting)
```

## Target (payment gateway flagship)

```
com.drippay
├── wallet                   (extract naming from generic "service" over time)
│   ├── WalletService
│   └── TransferService      P2P, demo deposit — posts ledger
├── pg                         ★ Payment gateway
│   ├── merchant             MID, API credentials, rotation
│   ├── order                pg_orders, idempotency
│   ├── payment              attempts, state machine
│   ├── checkout             session tokens for hosted UI
│   ├── refund
│   ├── settlement           batches, MDR
│   ├── webhook              inbound provider + outbound merchant (unify with merchant/)
│   └── provider             SimulatedPspAdapter (timeout, async callback)
├── ledger                   shared posting API used by wallet + pg
├── event                    outbox (rename optional)
├── finance                  reconciliation + pg settlement recon
└── ai                       MerchantOpsTools (read-only)
```

## HTTP path convention

| Prefix | Module |
|--------|--------|
| `/api/v1/auth`, `/users`, `/wallets`, `/transfers` | Wallet consumer |
| `/api/v1/pg/**` | Payment gateway (merchant HMAC or checkout session) |
| `/api/v1/admin/**` | Ops |
| `/api/v1/insights/**` | User AI (today) |

## Migration notes

1. Add `pg` tables via Flyway `V8__payment_gateway.sql` without breaking wallet tables.
2. Map existing `WebhookEndpoint` (per `user_id`) → `merchants` entity with MID; keep deliveries table shape.
3. `RazorpayService` → implement `pg.provider.RazorpayAdapter` or generic simulator first.
4. Enforce payment states in **one** `PgPaymentStateService` (wallet `TransactionStatus` stays for P2P).
