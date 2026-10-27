package za.co.sbsa.sbsa_fraud.domain.converter;

import jakarta.persistence.Converter;
import za.co.sbsa.sbsa_fraud.domain.TransactionStatus;

/** Stores {@link TransactionStatus} as its database text. */
@Converter
public class TransactionStatusConverter extends DbValueEnumConverter<TransactionStatus> {

    public TransactionStatusConverter() {
        super(TransactionStatus.class);
    }
}
