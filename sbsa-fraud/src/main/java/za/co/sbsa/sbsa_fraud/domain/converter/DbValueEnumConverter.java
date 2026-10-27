package za.co.sbsa.sbsa_fraud.domain.converter;

import jakarta.persistence.AttributeConverter;
import za.co.sbsa.sbsa_fraud.domain.DbValueEnum;

/**
 * Converts a {@link DbValueEnum} to and from its database text.
 * <p>
 * Used instead of {@code @Enumerated(EnumType.STRING)} for two reasons:
 * the stored values ("Flagged") differ from Java constant names (FLAGGED),
 * and Hibernate would otherwise expect a MySQL ENUM column instead of the VARCHAR
 * columns Liquibase created.
 *
 * @param <E> the enum type
 */
public abstract class DbValueEnumConverter<E extends Enum<E> & DbValueEnum>
        implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected DbValueEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return value == null ? null : value.getDbValue();
    }

    @Override
    public E convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.getDbValue().equals(dbValue)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(
                "Unknown " + type.getSimpleName() + " value in database: '" + dbValue + "'");
    }
}
