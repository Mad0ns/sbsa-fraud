package za.co.sbsa.sbsa_fraud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import za.co.sbsa.sbsa_fraud.domain.converter.AlertStatusConverter;

/**
 * Alert raised automatically when a fraud rule fires on a transaction (FR-08).
 * One alert per rule fired; the database rejects a second alert for the same pair.
 */
@Entity
@Table(name = "FraudAlert")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AlertID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TransactionID", nullable = false)
    @ToString.Exclude
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RuleID", nullable = false)
    @ToString.Exclude
    private FraudRule rule;

    @Convert(converter = AlertStatusConverter.class)
    @Column(name = "AlertStatus", nullable = false, length = 20)
    private AlertStatus status;

    /** Set by Hibernate when the alert is first saved. */
    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Raises a new {@link AlertStatus#OPEN} alert.
     *
     * @param transaction the flagged transaction
     * @param rule        the rule that fired
     */
    public FraudAlert(Transaction transaction, FraudRule rule) {
        this.transaction = transaction;
        this.rule = rule;
        this.status = AlertStatus.OPEN;
    }
}
