package za.co.sbsa.sbsa_fraud.domain.converter;

import jakarta.persistence.Converter;
import za.co.sbsa.sbsa_fraud.domain.AlertStatus;

/** Stores {@link AlertStatus} as its database text. */
@Converter
public class AlertStatusConverter extends DbValueEnumConverter<AlertStatus> {

    public AlertStatusConverter() {
        super(AlertStatus.class);
    }
}
