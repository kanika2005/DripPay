-- Transaction reversals, KYC, refresh tokens, outbox, webhooks, risk rules, rate limits, AI chat

ALTER TABLE transactions
    ADD COLUMN parent_txn_id BIGINT NULL,
    ADD CONSTRAINT fk_tx_parent FOREIGN KEY (parent_txn_id) REFERENCES transactions (id);

ALTER TABLE users
    ADD COLUMN kyc_tier VARCHAR(20) NOT NULL DEFAULT 'MIN_KYC',
    ADD COLUMN transfer_pin_hash VARCHAR(255) NULL;

CREATE TABLE refresh_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(64)  NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE outbox_events (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type   VARCHAR(64)  NOT NULL,
    payload      JSON         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    published_at TIMESTAMP    NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_outbox_pending (status, created_at)
);

CREATE TABLE webhook_endpoints (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    url        VARCHAR(512) NOT NULL,
    secret     VARCHAR(128) NOT NULL,
    active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE webhook_deliveries (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    endpoint_id BIGINT       NOT NULL,
    event_type  VARCHAR(64)  NOT NULL,
    payload     JSON         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts    INT          NOT NULL DEFAULT 0,
    last_error  VARCHAR(500) NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_endpoint FOREIGN KEY (endpoint_id) REFERENCES webhook_endpoints (id),
    INDEX idx_webhook_delivery_pending (status, created_at)
);

CREATE TABLE risk_rules (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL,
    description VARCHAR(255) NOT NULL,
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,
    config_json JSON         NULL,
    CONSTRAINT uk_risk_rule_code UNIQUE (code)
);

INSERT INTO risk_rules (code, description, enabled, config_json) VALUES
('VELOCITY_TRANSFER', 'Potentially suspicious: many transfers in short window', 1, '{"windowMinutes":10,"maxCount":8}'),
('HIGH_AMOUNT', 'Potentially suspicious: high transfer amount', 1, '{"threshold":10000}'),
('FAILED_ATTEMPTS', 'Potentially suspicious: repeated failed transfers', 1, '{"maxPerHour":5}');

CREATE TABLE rate_limit_counters (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NULL,
    action_key   VARCHAR(64)  NOT NULL,
    window_start TIMESTAMP    NOT NULL,
    count        INT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_rate_limit UNIQUE (user_id, action_key, window_start)
);

CREATE TABLE agent_conversations (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    role       VARCHAR(16)  NOT NULL,
    message    VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_conv_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_agent_conv_user (user_id, created_at DESC)
);
