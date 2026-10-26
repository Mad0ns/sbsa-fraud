--liquibase formatted sql

--changeset nkanyiso:005-1
--comment: Append-only audit of every fraud decision (FR-09)
CREATE TABLE DecisionLog (
    LogID           BIGINT      NOT NULL AUTO_INCREMENT,
    TransactionID   INT         NOT NULL,
    RulesEvaluated  SMALLINT    NOT NULL,
    RulesFired      SMALLINT    NOT NULL,
    Decision        VARCHAR(20) NOT NULL,
    ProcessingMs    INT         NOT NULL,
    LoggedAt        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_decision_log PRIMARY KEY (LogID),
    CONSTRAINT uq_decision_txn UNIQUE (TransactionID),
    CONSTRAINT fk_decision_txn FOREIGN KEY (TransactionID) REFERENCES `Transaction` (TransactionID) ON DELETE RESTRICT,
    CONSTRAINT chk_decision      CHECK (Decision IN ('Success','Flagged')),
    CONSTRAINT chk_rules_counts  CHECK (RulesEvaluated >= 0 AND RulesFired >= 0 AND RulesFired <= RulesEvaluated),
    CONSTRAINT chk_processing_ms CHECK (ProcessingMs >= 0)
) ENGINE=InnoDB;
--rollback DROP TABLE DecisionLog;

-- NFR-03 is also enforced inside the database: even a manual UPDATE or DELETE fails.
-- Trigger bodies contain semicolons, so these changesets turn statement splitting off.

--changeset nkanyiso:005-2 splitStatements:false
--comment: Block updates to DecisionLog
CREATE TRIGGER trg_decision_log_no_update
BEFORE UPDATE ON DecisionLog
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DecisionLog is append-only: updates are not allowed';
--rollback DROP TRIGGER trg_decision_log_no_update;

--changeset nkanyiso:005-3 splitStatements:false
--comment: Block deletes from DecisionLog
CREATE TRIGGER trg_decision_log_no_delete
BEFORE DELETE ON DecisionLog
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DecisionLog is append-only: deletes are not allowed';
--rollback DROP TRIGGER trg_decision_log_no_delete;
