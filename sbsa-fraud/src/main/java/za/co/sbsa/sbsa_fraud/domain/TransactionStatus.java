package za.co.sbsa.sbsa_fraud.domain;

/**
 * Processing status of a transaction. Stored in {@code Transaction.Status}.
 */
public enum TransactionStatus implements DbValueEnum {

    SUCCESS("Success"),
    PENDING("Pending"),
    FLAGGED("Flagged"),
    FAILED("Failed");

    private final String dbValue;

    TransactionStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }
}
