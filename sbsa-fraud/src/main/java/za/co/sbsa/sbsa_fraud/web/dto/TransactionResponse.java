package za.co.sbsa.sbsa_fraud.web.dto;

import java.util.List;

/**
 * Result returned to the channel after processing (FR-10).
 *
 * @param transactionRef the reference that was processed
 * @param decision       "Success" or "Flagged"
 * @param rulesFired     codes of the fraud rules that fired; empty when none did
 * @param processingMs   time taken to reach the decision
 */
public record TransactionResponse(
        String transactionRef,
        String decision,
        List<String> rulesFired,
        long processingMs) {
}
