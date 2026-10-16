# Wallet Service — Phase 1 Learning Project

Simple Wallet Management REST API (Java 21 + Spring Boot 3 + Gradle + MySQL + Flyway).

## Run locally

Use **Java 21** (required by this project):

```bash
export JAVA_HOME="/Users/kanikakholia/Library/Java/JavaVirtualMachines/temurin-21.0.11/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

1. Start MySQL and create DB (or let JDBC create it):
   ```text
   DB name: wallet_db
   ```
2. Credentials (optional if root has no password):
   ```bash
   export DB_USERNAME=root
   export DB_PASSWORD=          # leave empty for `mysql -u root` with no password
   ```
   In **IntelliJ**: Run → Edit Configurations → your `WalletApplication` → Environment → remove `DB_PASSWORD` or set it to blank (a leftover `password` value causes `Access denied … using password: YES`).
3. From `wallet-service/`:
   ```bash
   ./gradlew bootRun
   ```
4. Run unit tests:
   ```bash
   ./gradlew test
   ```

## API quick reference

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/v1/wallets` | Create wallet |
| GET | `/api/v1/wallets/{id}` | Get wallet / balance |
| POST | `/api/v1/wallets/{id}/credit` | Add money |
| POST | `/api/v1/wallets/{id}/debit` | Withdraw money |
| GET | `/api/v1/wallets/{id}/transactions` | Transaction history |
