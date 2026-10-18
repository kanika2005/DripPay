-- One ledger row per (transaction, wallet); align velocity rule with validation spec

ALTER TABLE ledger_entries
    ADD CONSTRAINT uk_ledger_tx_wallet UNIQUE (transaction_id, wallet_id);

UPDATE risk_rules
SET description = 'Potentially suspicious: many transfers in short window',
    config_json = '{"windowMinutes":1,"maxCount":5}'
WHERE code = 'VELOCITY_TRANSFER';

UPDATE risk_rules
SET description = 'Potentially suspicious: high transfer amount'
WHERE code = 'HIGH_AMOUNT';

UPDATE risk_rules
SET description = 'Potentially suspicious: repeated failed transfers'
WHERE code = 'FAILED_ATTEMPTS';
