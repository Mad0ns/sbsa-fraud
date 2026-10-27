package za.co.sbsa.sbsa_fraud.domain;

/**
 * Fraud risk rating supplied with the WM 02 source data. Stored in {@code Transaction.RiskLevel}.
 */
public enum RiskLevel implements DbValueEnum {

    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    private final String dbValue;

    RiskLevel(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }
}
