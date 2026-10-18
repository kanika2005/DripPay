-- Collect handles, payment drafts, Razorpay test orders

ALTER TABLE users
    ADD COLUMN drip_handle VARCHAR(64) NULL,
    ADD CONSTRAINT uk_users_drip_handle UNIQUE (drip_handle);

CREATE TABLE payment_drafts (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT         NOT NULL,
    recipient_handle    VARCHAR(64)    NULL,
    recipient_wallet_id VARCHAR(32)    NULL,
    amount              DECIMAL(19, 4) NOT NULL,
    note                VARCHAR(255)   NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    source              VARCHAR(32)    NOT NULL DEFAULT 'USER',
    expires_at          TIMESTAMP      NOT NULL,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_draft_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_draft_user_status (user_id, status)
);

CREATE TABLE razorpay_orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    razorpay_order_id VARCHAR(64)  NOT NULL,
    amount_paise    BIGINT         NOT NULL,
    currency        VARCHAR(8)     NOT NULL DEFAULT 'INR',
    status          VARCHAR(20)    NOT NULL DEFAULT 'CREATED',
    simulated       TINYINT(1)     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at         TIMESTAMP      NULL,
    CONSTRAINT uk_razorpay_order UNIQUE (razorpay_order_id),
    CONSTRAINT fk_razorpay_user FOREIGN KEY (user_id) REFERENCES users (id)
);
