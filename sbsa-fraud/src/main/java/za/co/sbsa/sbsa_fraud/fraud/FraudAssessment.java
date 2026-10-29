package za.co.sbsa.sbsa_fraud.fraud;

import java.util.List;
import za.co.sbsa.sbsa_fraud.domain.FraudRule;

/**
 * Result of screening one transaction.
 *
 * @param rulesEvaluated number of active rules that were checked
 * @param firedRules     the rules that fired; empty when the transaction is clean
 */
public record FraudAssessment(int rulesEvaluated, List<FraudRule> firedRules) {

    public FraudAssessment {
        firedRules = List.copyOf(firedRules);
    }

    /** @return {@code true} if at least one rule fired, so the transaction must be held (FR-07) */
    public boolean fraudSuspected() {
        return !firedRules.isEmpty();
    }
}