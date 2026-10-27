package za.co.sbsa.sbsa_fraud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import za.co.sbsa.sbsa_fraud.domain.converter.DecisionConverter;

/**
 * Audit record of one fraud decision (FR-09).
 * <p>
 * Append-only at three levels (NFR-03): {@code @Immutable} stops Hibernate updating it,
 * {@code DecisionLogRepository} exposes no delete, and database triggers reject any
 * UPDATE or DELETE.
 */
@Entity
@Immutable
@Table(name = "DecisionLog")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class DecisionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogID")
    private Long id;

    /** Exactly one log row per processed transaction. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TransactionID", nullable = false, unique = true)
    @ToString.Exclude
    private Transaction transaction;

    /** How many active rules were checked. SMALLINT column, so {@code Short}. */
    @Column(name = "RulesEvaluated", nullable = false)
    private Short rulesEvaluated;

    /** How many of those rules fired. */
    @Column(name = "RulesFired", nullable = false)
    private Short rulesFired;

    @Convert(converter = DecisionConverter.class)
    @Column(name = "Decision", nullable = false, length = 20)
    private Decision decision;

    /** Time taken to reach the decision, for the NFR-01 latency target. */
    @Column(name = "ProcessingMs", nullable = false)
    private Integer processingMs;

    /** Set by Hibernate when the row is saved. */
    @CreationTimestamp
    @Column(name = "LoggedAt", nullable = false, updatable = false)
    private Instant loggedAt;

    /**
     * Records the outcome of screening one transaction.
     *
     * @param transaction    the transaction that was screened
     * @param rulesEvaluated number of active rules checked
     * @param rulesFired     number of rules that fired
     * @param decision       the outcome
     * @param processingMs   elapsed time in milliseconds
     */
    public DecisionLog(Transaction transaction, short rulesEvaluated, short rulesFired,
                       Decision decision, int processingMs) {
        this.transaction = transaction;
        this.rulesEvaluated = rulesEvaluated;
        this.rulesFired = rulesFired;
        this.decision = decision;
        this.processingMs = processingMs;
    }
}
