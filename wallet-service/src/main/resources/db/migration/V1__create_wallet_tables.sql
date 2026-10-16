-- V1__create_wallet_tables.sql
-- Flyway runs this automatically on application startup (in version order).
-- File name pattern: V{version}__{description}.sql

CREATE TABLE wallet (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT         NOT NULL,
    balance    DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- One wallet per user in this simple Phase 1 design
    CONSTRAINT uk_wallet_user_id UNIQUE (user_id)
);

CREATE TABLE wallet_transaction (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id  BIGINT         NOT NULL,
    type       VARCHAR(10)    NOT NULL,  -- CREDIT or DEBIT
    amount     DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wallet_transaction_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallet (id)
);

CREATE INDEX idx_wallet_transaction_wallet_id ON wallet_transaction (wallet_id);
