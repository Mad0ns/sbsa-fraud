package za.co.sbsa.sbsa_fraud.domain.converter;

import jakarta.persistence.Converter;
import za.co.sbsa.sbsa_fraud.domain.RiskLevel;

/** Stores {@link RiskLevel} as its database text. */
@Converter
public class RiskLevelConverter extends DbValueEnumConverter<RiskLevel> {

    public RiskLevelConverter() {
        super(RiskLevel.class);
    }
}
