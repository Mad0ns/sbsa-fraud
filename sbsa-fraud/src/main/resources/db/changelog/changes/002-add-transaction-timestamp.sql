--liquibase formatted sql

--changeset nkanyiso:002-1
--comment: Velocity needs time, not just a date (FR-05). Millisecond precision, stored in UTC (Q6).
ALTER TABLE `Transaction`
    ADD COLUMN TransactionTimestamp DATETIME(3) NOT NULL AFTER TransactionDate;
--rollback ALTER TABLE `Transaction` DROP COLUMN TransactionTimestamp;

--changeset nkanyiso:002-2
--comment: Velocity window lookup - all transactions for one account in a time range (NFR-01)
CREATE INDEX idx_txn_account_time ON `Transaction` (AccountID, TransactionTimestamp);
--rollback DROP INDEX idx_txn_account_time ON `Transaction`;
