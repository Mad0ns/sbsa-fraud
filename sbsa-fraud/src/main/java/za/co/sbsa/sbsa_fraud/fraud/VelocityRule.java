package za.co.sbsa.sbsa_fraud.fraud;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.co.sbsa.sbsa_fraud.domain.FraudRule;
import za.co.sbsa.sbsa_fraud.domain.Transaction;
import za.co.sbsa.sbsa_fraud.repository.TransactionRepository;

/**
 * Velocity rule: catches card testing, where many small transactions hit one account
 * in a short time (FR-05, US-03).
 * <p>
 * With the agreed thresholds (Q2, Q4, Q5) it fires when the incoming transaction is
 * R100 or less and, counting itself, the account has 5 or more such transactions in
 * the rolling 10 minutes ending at its timestamp. Every stored transaction counts,
 * whatever its status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VelocityRule implements FraudRuleEvaluator {

    public static final String RULE_CODE = "VELOCITY_MICRO";

    private final TransactionRepository transactionRepository;

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public boolean isTriggered(Transaction transaction, FraudRule rule) {
        if (rule.getMaxCount() == null || rule.getAmountCeiling() == null || rule.getWindowMinutes() == null) {
            log.warn("Rule {} is missing a threshold and was not applied", rule.getRuleCode());
            return false;
        }

        // Only a micro-transaction can complete a burst of micro-transactions.
        if (transaction.getAmount().compareTo(rule.getAmountCeiling()) > 0) {
            return false;
        }

        Instant windowEnd = transaction.getTransactionTimestamp();
        Instant windowStart = windowEnd.minus(Duration.ofMinutes(rule.getWindowMinutes()));

        long earlier = transactionRepository.countMicroTransactionsInWindow(
                transaction.getAccount(), rule.getAmountCeiling(), windowStart, windowEnd);

        // +1: the incoming transaction is not saved yet, but it counts towards the limit.
        return earlier + 1 >= rule.getMaxCount();
    }
}
