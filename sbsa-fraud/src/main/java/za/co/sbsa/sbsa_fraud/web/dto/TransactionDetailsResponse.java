package za.co.sbsa.sbsa_fraud.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Body of {@code GET /api/transactions/{ref}} (FR-11, US-07).
 *
 * @param transactionRef the reference
 * @param accountNumber  account the transaction was posted to
 * @param amount         amount in ZAR
 * @param timestamp      when the channel recorded it (UTC)
 * @param status         "Success", "Flagged", "Pending" or "Failed"
 * @param alerts         fraud alerts raised for it; empty when none
 */
public record TransactionDetailsResponse(
        String transactionRef,
        String accountNumber,
        BigDecimal amount,
        Instant timestamp,
        String status,
        List<AlertSummary> alerts) {

    /**
     * One alert on the transaction.
     *
     * @param ruleCode  the rule that fired
     * @param status    "Open", "Investigating" or "Closed"
     * @param createdAt when the alert was raised
     */
    public record AlertSummary(String ruleCode, String status, Instant createdAt) {
    }
}
