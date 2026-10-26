--liquibase formatted sql

--changeset nkanyiso:003-1
--comment: Rule thresholds live in data, not code, so they change without a release (FR-06)
CREATE TABLE FraudRule (
    RuleID         INT           NOT NULL AUTO_INCREMENT,
    RuleCode       VARCHAR(40)   NOT NULL,
    Description    VARCHAR(255)  NOT NULL,
    MaxCount       INT           NULL,
    AmountCeiling  DECIMAL(15,2) NULL,
    WindowMinutes  INT           NULL,
    IsActive       BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_fraud_rule      PRIMARY KEY (RuleID),
    CONSTRAINT uq_fraud_rule_code UNIQUE (RuleCode),
    CONSTRAINT chk_rule_count  CHECK (MaxCount IS NULL OR MaxCount > 0),
    CONSTRAINT chk_rule_amount CHECK (AmountCeiling IS NULL OR AmountCeiling > 0),
    CONSTRAINT chk_rule_window CHECK (WindowMinutes IS NULL OR WindowMinutes > 0)
) ENGINE=InnoDB;
--rollback DROP TABLE FraudRule;

--changeset nkanyiso:003-2
--comment: Velocity rule with the thresholds agreed in Q2 (5 x R100 or less in 10 minutes)
INSERT INTO FraudRule (RuleCode, Description, MaxCount, AmountCeiling, WindowMinutes, IsActive)
VALUES ('VELOCITY_MICRO',
        'Five or more transactions of R100 or less on one account within 10 minutes',
        5, 100.00, 10, TRUE);
--rollback DELETE FROM FraudRule WHERE RuleCode = 'VELOCITY_MICRO';
