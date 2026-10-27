package za.co.sbsa.sbsa_fraud.domain;

/**
 * Outcome of fraud screening for one transaction (FR-07). Stored in {@code DecisionLog.Decision}.
 */
public enum Decision implements DbValueEnum {

    SUCCESS("Success"),
    FLAGGED("Flagged");

    private final String dbValue;

    Decision(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }
}
