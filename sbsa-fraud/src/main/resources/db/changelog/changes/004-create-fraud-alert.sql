--liquibase formatted sql

--changeset nkanyiso:004-1
--comment: One alert per rule fired on a transaction (FR-08)
CREATE TABLE FraudAlert (
    AlertID        BIGINT      NOT NULL AUTO_INCREMENT,
    TransactionID  INT         NOT NULL,
    RuleID         INT         NOT NULL,
    AlertStatus    VARCHAR(20) NOT NULL DEFAULT 'Open',
    CreatedAt      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_fraud_alert PRIMARY KEY (AlertID),
    CONSTRAINT uq_alert_txn_rule UNIQUE (TransactionID, RuleID),
    CONSTRAINT fk_alert_txn  FOREIGN KEY (TransactionID) REFERENCES `Transaction` (TransactionID) ON DELETE RESTRICT,
    CONSTRAINT fk_alert_rule FOREIGN KEY (RuleID)        REFERENCES FraudRule (RuleID)          ON DELETE RESTRICT,
    CONSTRAINT chk_alert_status CHECK (AlertStatus IN ('Open','Investigating','Closed')),
    INDEX idx_alert_status (AlertStatus)
) ENGINE=InnoDB;
--rollback DROP TABLE FraudAlert;
