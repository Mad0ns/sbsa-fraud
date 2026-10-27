package za.co.sbsa.sbsa_fraud.domain;

/**
 * An enum whose constants are stored in the database as fixed text values,
 * for example {@code FLAGGED} stored as {@code "Flagged"}.
 * <p>
 * The database keeps the values used in the source data (CHECK constraints in
 * the Liquibase changesets); Java keeps its UPPER_CASE naming convention.
 */
public interface DbValueEnum {

    /** @return the exact text stored in the database column */
    String getDbValue();
}
