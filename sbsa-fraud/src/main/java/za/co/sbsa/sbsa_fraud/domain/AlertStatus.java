package za.co.sbsa.sbsa_fraud.domain;

/**
 * Life cycle of a fraud alert. Stored in {@code FraudAlert.AlertStatus}.
 */
public enum AlertStatus implements DbValueEnum {

    OPEN("Open"),
    INVESTIGATING("Investigating"),
    CLOSED("Closed");

    private final String dbValue;

    AlertStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }
}
