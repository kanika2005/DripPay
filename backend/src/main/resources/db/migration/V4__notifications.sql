CREATE TABLE notifications (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    type              VARCHAR(32)  NOT NULL,
    title             VARCHAR(200) NOT NULL,
    body              VARCHAR(500) NOT NULL,
    group_id          BIGINT       NULL,
    group_expense_id  BIGINT       NULL,
    read_at           TIMESTAMP    NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
