package za.co.sbsa.sbsa_fraud.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Body of {@code POST /api/transactions} (FR-01).
 * <p>
 * The annotations check the <em>format</em> of the input before any code runs.
 * Whether the account, channel and so on actually <em>exist</em> is checked
 * against the database by {@code TransactionService} (FR-02).
 *
 * @param transactionRef  unique reference from the channel, e.g. "TX-000123"
 * @param accountNumber   account to post to
 * @param amount          amount in ZAR, at least 0.01, at most 2 decimal places
 * @param channel         channel name, e.g. "ATM"
 * @param transactionType type name, e.g. "POS Purchase"
 * @param city            city name, optional
 * @param timestamp       when the channel recorded the transaction, ISO-8601 UTC,
 *                        e.g. "2026-10-27T09:00:00Z"
 */
public record TransactionRequest(
        @NotBlank @Size(max = 20) String transactionRef,
        @NotBlank @Size(max = 20) String accountNumber,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotBlank String channel,
        @NotBlank String transactionType,
        String city,
        @NotNull Instant timestamp) {
}
