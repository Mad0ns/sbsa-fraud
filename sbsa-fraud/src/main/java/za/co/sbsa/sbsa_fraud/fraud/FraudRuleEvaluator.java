package za.co.sbsa.sbsa_fraud.fraud;

import za.co.sbsa.sbsa_fraud.domain.FraudRule;
import za.co.sbsa.sbsa_fraud.domain.Transaction;

/**
 * Contract every fraud rule implements.
 * <p>
 * A new rule is added by writing one class that implements this interface (as a Spring
 * {@code @Component}) and inserting a matching row in the FraudRule table. The
 * {@link FraudEngine} finds it automatically; nothing else changes.
 */
public interface FraudRuleEvaluator {

    /** @return the {@code FraudRule.RuleCode} this evaluator implements, e.g. VELOCITY_MICRO */
    String ruleCode();

    /**
     * Decides whether the rule fires for a transaction that has not been saved yet.
     *
     * @param transaction the incoming transaction
     * @param rule        the rule row, with the thresholds to apply
     * @return {@code true} if the transaction should be held
     */
    boolean isTriggered(Transaction transaction, FraudRule rule);
}
