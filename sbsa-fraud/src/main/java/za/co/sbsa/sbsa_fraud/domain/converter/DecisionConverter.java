package za.co.sbsa.sbsa_fraud.domain.converter;

import jakarta.persistence.Converter;
import za.co.sbsa.sbsa_fraud.domain.Decision;

/** Stores {@link Decision} as its database text. */
@Converter
public class DecisionConverter extends DbValueEnumConverter<Decision> {

    public DecisionConverter() {
        super(Decision.class);
    }
}
