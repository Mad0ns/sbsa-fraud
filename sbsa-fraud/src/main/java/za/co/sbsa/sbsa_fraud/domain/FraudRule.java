package za.co.sbsa.sbsa_fraud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Immutable;

/**
 * A fraud detection rule and its thresholds (FR-06).
 * <p>
 * Thresholds live in the database so the fraud team can tune them without a code
 * release; the application only reads them. Which threshold columns a rule uses
 * depends on its {@link #ruleCode}; unused ones are {@code null}.
 */
@Entity
@Immutable
@Table(name = "FraudRule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RuleID")
    private Integer id;

    /** Stable identifier the code uses to find its rule, e.g. VELOCITY_MICRO. */
    @Column(name = "RuleCode", nullable = false, unique = true, length = 40)
    private String ruleCode;

    @Column(name = "Description", nullable = false, length = 255)
    private String description;

    /** Number of transactions at which the rule fires. */
    @Column(name = "MaxCount")
    private Integer maxCount;

    /** Largest amount (ZAR) that still counts as a micro-transaction. */
    @Column(name = "AmountCeiling", precision = 15, scale = 2)
    private BigDecimal amountCeiling;

    /** Length of the rolling time window, in minutes. */
    @Column(name = "WindowMinutes")
    private Integer windowMinutes;

    /** Inactive rules are not evaluated (US-06). */
    @Column(name = "IsActive", nullable = false)
    private Boolean active;
}
