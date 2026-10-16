-- DripPay core schema (Flyway)

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE wallets (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    public_wallet_id VARCHAR(12)   NOT NULL,
    balance         DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_wallets_user_id UNIQUE (user_id),
    CONSTRAINT uk_wallets_public_id UNIQUE (public_wallet_id),
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_wallets_balance_nonneg CHECK (balance >= 0)
);

CREATE TABLE transaction_categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL,
    label       VARCHAR(80)  NOT NULL,
    CONSTRAINT uk_transaction_categories_code UNIQUE (code)
);

INSERT INTO transaction_categories (code, label) VALUES
('FOOD', 'Food & Dining'),
('SHOPPING', 'Shopping'),
('TRANSPORT', 'Transport'),
('ENTERTAINMENT', 'Entertainment'),
('BILLS', 'Bills & Utilities'),
('TRANSFER', 'Transfers'),
('OTHER', 'Other');

CREATE TABLE transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_id        VARCHAR(36)    NOT NULL,
    type                VARCHAR(30)    NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    amount              DECIMAL(19, 4) NOT NULL,
    currency            VARCHAR(3)     NOT NULL DEFAULT 'INR',
    sender_wallet_id    BIGINT         NULL,
    recipient_wallet_id BIGINT         NULL,
    initiated_by_user_id BIGINT        NOT NULL,
    category_id         BIGINT         NULL,
    description         VARCHAR(255)   NULL,
    idempotency_key     VARCHAR(64)    NULL,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_transactions_reference UNIQUE (reference_id),
    CONSTRAINT fk_tx_sender_wallet FOREIGN KEY (sender_wallet_id) REFERENCES wallets (id),
    CONSTRAINT fk_tx_recipient_wallet FOREIGN KEY (recipient_wallet_id) REFERENCES wallets (id),
    CONSTRAINT fk_tx_user FOREIGN KEY (initiated_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_tx_category FOREIGN KEY (category_id) REFERENCES transaction_categories (id)
);

CREATE INDEX idx_transactions_sender ON transactions (sender_wallet_id, created_at DESC);
CREATE INDEX idx_transactions_recipient ON transactions (recipient_wallet_id, created_at DESC);
CREATE INDEX idx_transactions_user ON transactions (initiated_by_user_id, created_at DESC);

CREATE TABLE ledger_entries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id  BIGINT         NOT NULL,
    wallet_id       BIGINT         NOT NULL,
    entry_type      VARCHAR(10)    NOT NULL,
    amount          DECIMAL(19, 4) NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_tx FOREIGN KEY (transaction_id) REFERENCES transactions (id),
    CONSTRAINT fk_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT chk_ledger_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX idx_ledger_wallet ON ledger_entries (wallet_id, created_at DESC);

CREATE TABLE idempotency_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    idempotency_key VARCHAR(64)  NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    transaction_id  BIGINT       NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_idempotency_user_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_idempotency_tx FOREIGN KEY (transaction_id) REFERENCES transactions (id)
);

CREATE TABLE budgets (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    category_id     BIGINT         NOT NULL,
    monthly_limit   DECIMAL(19, 4) NOT NULL,
    month_year      VARCHAR(7)     NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_budget_user_cat_month UNIQUE (user_id, category_id, month_year),
    CONSTRAINT fk_budget_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES transaction_categories (id)
);

CREATE TABLE expense_groups (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    created_by_user_id BIGINT    NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_creator FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);

CREATE TABLE group_members (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id        BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    joined_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_group_member UNIQUE (group_id, user_id),
    CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES expense_groups (id),
    CONSTRAINT fk_gm_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE group_expenses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id        BIGINT         NOT NULL,
    paid_by_user_id BIGINT         NOT NULL,
    title           VARCHAR(160)   NOT NULL,
    total_amount    DECIMAL(19, 4) NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ge_group FOREIGN KEY (group_id) REFERENCES expense_groups (id),
    CONSTRAINT fk_ge_payer FOREIGN KEY (paid_by_user_id) REFERENCES users (id)
);

CREATE TABLE expense_shares (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_expense_id BIGINT        NOT NULL,
    user_id         BIGINT         NOT NULL,
    share_amount    DECIMAL(19, 4) NOT NULL,
    CONSTRAINT uk_expense_share UNIQUE (group_expense_id, user_id),
    CONSTRAINT fk_es_expense FOREIGN KEY (group_expense_id) REFERENCES group_expenses (id),
    CONSTRAINT fk_es_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE settlements (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id        BIGINT         NOT NULL,
    from_user_id    BIGINT         NOT NULL,
    to_user_id      BIGINT         NOT NULL,
    amount          DECIMAL(19, 4) NOT NULL,
    transaction_id  BIGINT         NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_settlement_group FOREIGN KEY (group_id) REFERENCES expense_groups (id),
    CONSTRAINT fk_settlement_from FOREIGN KEY (from_user_id) REFERENCES users (id),
    CONSTRAINT fk_settlement_to FOREIGN KEY (to_user_id) REFERENCES users (id),
    CONSTRAINT fk_settlement_tx FOREIGN KEY (transaction_id) REFERENCES transactions (id)
);

CREATE TABLE security_alerts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    severity        VARCHAR(20)  NOT NULL,
    reason          VARCHAR(500) NOT NULL,
    review_status   VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at     TIMESTAMP    NULL,
    reviewed_by_user_id BIGINT   NULL,
    CONSTRAINT fk_alert_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_alert_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id)
);

CREATE INDEX idx_security_alerts_user ON security_alerts (user_id, created_at DESC);

CREATE TABLE audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NULL,
    action          VARCHAR(80)  NOT NULL,
    details         VARCHAR(1000) NULL,
    ip_address      VARCHAR(45)  NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_audit_created ON audit_logs (created_at DESC);
