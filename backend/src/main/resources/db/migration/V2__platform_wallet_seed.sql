-- Platform float wallet for demo deposits/withdrawals (double-entry contra account)
INSERT INTO users (id, email, password_hash, full_name, role)
VALUES (1, 'platform@drippay.internal', '$2a$10$platformplaceholderhashnotusedforlogin', 'DripPay Platform', 'ADMIN');

INSERT INTO wallets (id, user_id, public_wallet_id, balance, version)
VALUES (1, 1, 'DRIPPLATFORM', 999999999.0000, 0);

ALTER TABLE users AUTO_INCREMENT = 2;
ALTER TABLE wallets AUTO_INCREMENT = 2;
