--liquibase formatted sql

-- Baseline: the final WM 02 schema (after WA0103), converted from T-SQL to MySQL.
--   IDENTITY(1,1)   -> AUTO_INCREMENT
--   [Transaction]   -> `Transaction`
--   Account.CustomerID was retired in WM 02 WA0103; ownership lives in AccountHolder.

--changeset nkanyiso:001-1
--comment: Customer lookup
CREATE TABLE Customer (
    CustomerID  INT          NOT NULL AUTO_INCREMENT,
    FullName    VARCHAR(120) NOT NULL,
    CONSTRAINT pk_customer      PRIMARY KEY (CustomerID),
    CONSTRAINT uq_customer_name UNIQUE (FullName)
) ENGINE=InnoDB;
--rollback DROP TABLE Customer;

--changeset nkanyiso:001-2
--comment: Channel lookup
CREATE TABLE Channel (
    ChannelID    INT         NOT NULL AUTO_INCREMENT,
    ChannelName  VARCHAR(40) NOT NULL,
    CONSTRAINT pk_channel      PRIMARY KEY (ChannelID),
    CONSTRAINT uq_channel_name UNIQUE (ChannelName)
) ENGINE=InnoDB;
--rollback DROP TABLE Channel;

--changeset nkanyiso:001-3
--comment: Location lookup
CREATE TABLE Location (
    LocationID  INT         NOT NULL AUTO_INCREMENT,
    City        VARCHAR(80) NOT NULL,
    CONSTRAINT pk_location PRIMARY KEY (LocationID),
    CONSTRAINT uq_location UNIQUE (City)
) ENGINE=InnoDB;
--rollback DROP TABLE Location;

--changeset nkanyiso:001-4
--comment: TransactionType lookup
CREATE TABLE TransactionType (
    TransactionTypeID  INT         NOT NULL AUTO_INCREMENT,
    TypeName           VARCHAR(40) NOT NULL,
    CONSTRAINT pk_txntype      PRIMARY KEY (TransactionTypeID),
    CONSTRAINT uq_txntype_name UNIQUE (TypeName)
) ENGINE=InnoDB;
--rollback DROP TABLE TransactionType;

--changeset nkanyiso:001-5
--comment: Account
CREATE TABLE Account (
    AccountID      INT         NOT NULL AUTO_INCREMENT,
    AccountNumber  VARCHAR(20) NOT NULL,
    CONSTRAINT pk_account        PRIMARY KEY (AccountID),
    CONSTRAINT uq_account_number UNIQUE (AccountNumber)
) ENGINE=InnoDB;
--rollback DROP TABLE Account;

--changeset nkanyiso:001-6
--comment: AccountHolder junction - resolves the Customer/Account many-to-many (WM 02 WA0103)
CREATE TABLE AccountHolder (
    CustomerID  INT         NOT NULL,
    AccountID   INT         NOT NULL,
    HolderRole  VARCHAR(20) NOT NULL DEFAULT 'Primary',
    AddedDate   DATE        NULL,
    CONSTRAINT pk_accountholder PRIMARY KEY (CustomerID, AccountID),
    CONSTRAINT fk_ah_customer FOREIGN KEY (CustomerID)
        REFERENCES Customer (CustomerID) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_ah_account  FOREIGN KEY (AccountID)
        REFERENCES Account (AccountID)   ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_ah_role CHECK (HolderRole IN ('Primary','Joint','Signatory')),
    INDEX idx_ah_account (AccountID)
) ENGINE=InnoDB;
--rollback DROP TABLE AccountHolder;

--changeset nkanyiso:001-7
--comment: Transaction fact table (reserved word, so always back-quoted)
CREATE TABLE `Transaction` (
    TransactionID      INT           NOT NULL AUTO_INCREMENT,
    TransactionRef     VARCHAR(20)   NOT NULL,
    AccountID          INT           NOT NULL,
    ChannelID          INT           NOT NULL,
    LocationID         INT           NULL,
    TransactionTypeID  INT           NULL,
    Amount             DECIMAL(15,2) NOT NULL,
    TransactionDate    DATE          NOT NULL,
    Status             VARCHAR(20)   NOT NULL,
    RiskLevel          VARCHAR(20)   NULL,
    CONSTRAINT pk_transaction     PRIMARY KEY (TransactionID),
    CONSTRAINT uq_transaction_ref UNIQUE (TransactionRef),
    CONSTRAINT fk_txn_account  FOREIGN KEY (AccountID)
        REFERENCES Account (AccountID)                 ON DELETE RESTRICT,
    CONSTRAINT fk_txn_channel  FOREIGN KEY (ChannelID)
        REFERENCES Channel (ChannelID)                 ON DELETE RESTRICT,
    CONSTRAINT fk_txn_location FOREIGN KEY (LocationID)
        REFERENCES Location (LocationID)               ON DELETE SET NULL,
    CONSTRAINT fk_txn_type     FOREIGN KEY (TransactionTypeID)
        REFERENCES TransactionType (TransactionTypeID) ON DELETE SET NULL,
    CONSTRAINT chk_txn_amount CHECK (Amount > 0),
    CONSTRAINT chk_txn_status CHECK (Status IN ('Success','Pending','Flagged','Failed')),
    CONSTRAINT chk_txn_risk   CHECK (RiskLevel IS NULL OR RiskLevel IN ('Low','Medium','High')),
    INDEX idx_txn_account (AccountID),   -- explicit, so MySQL never repurposes another index for this FK
    INDEX idx_txn_status (Status),
    INDEX idx_txn_risk   (RiskLevel),
    INDEX idx_txn_date   (TransactionDate)
) ENGINE=InnoDB;
--rollback DROP TABLE `Transaction`;
