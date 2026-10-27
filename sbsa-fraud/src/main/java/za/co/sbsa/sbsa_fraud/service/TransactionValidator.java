package za.co.sbsa.sbsa_fraud.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.sbsa.sbsa_fraud.domain.Account;
import za.co.sbsa.sbsa_fraud.domain.Channel;
import za.co.sbsa.sbsa_fraud.domain.Location;
import za.co.sbsa.sbsa_fraud.domain.Transaction;
import za.co.sbsa.sbsa_fraud.domain.TransactionType;
import za.co.sbsa.sbsa_fraud.repository.AccountRepository;
import za.co.sbsa.sbsa_fraud.repository.ChannelRepository;
import za.co.sbsa.sbsa_fraud.repository.LocationRepository;
import za.co.sbsa.sbsa_fraud.repository.TransactionTypeRepository;
import za.co.sbsa.sbsa_fraud.service.exception.InvalidTransactionException;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionRequest;

/**
 * Business validation of a well-formed request (FR-02).
 * <p>
 * Format checks (blank fields, amount above zero) have already been done by the
 * annotations on {@link TransactionRequest}; this class checks the request against the
 * clock and the database, and turns it into a {@link Transaction} ready for screening.
 */
@Component
@RequiredArgsConstructor
public class TransactionValidator {

    /** Channel clocks may run slightly fast; anything further in the future is rejected (Q6). */
    static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

    private final AccountRepository accountRepository;
    private final ChannelRepository channelRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final LocationRepository locationRepository;
    private final Clock clock;

    /**
     * Checks the request and builds a new transaction in PENDING state.
     *
     * @param request the incoming request
     * @return an unsaved transaction
     * @throws InvalidTransactionException if the timestamp is in the future or any
     *                                     referenced account, channel, type or city is unknown
     */
    public Transaction toValidTransaction(TransactionRequest request) {
        Instant latestAllowed = Instant.now(clock).plus(MAX_CLOCK_SKEW);
        if (request.timestamp().isAfter(latestAllowed)) {
            throw new InvalidTransactionException("INVALID_TIMESTAMP",
                    "timestamp " + request.timestamp() + " is more than "
                            + MAX_CLOCK_SKEW.toMinutes() + " minutes in the future");
        }

        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new InvalidTransactionException("INVALID_ACCOUNT",
                        "Account " + request.accountNumber() + " does not exist"));

        Channel channel = channelRepository.findByName(request.channel())
                .orElseThrow(() -> new InvalidTransactionException("INVALID_CHANNEL",
                        "Channel '" + request.channel() + "' is not recognised"));

        TransactionType type = transactionTypeRepository.findByTypeName(request.transactionType())
                .orElseThrow(() -> new InvalidTransactionException("INVALID_TRANSACTION_TYPE",
                        "Transaction type '" + request.transactionType() + "' is not recognised"));

        Location location = null;
        if (request.city() != null && !request.city().isBlank()) {
            location = locationRepository.findByCity(request.city())
                    .orElseThrow(() -> new InvalidTransactionException("INVALID_CITY",
                            "City '" + request.city() + "' is not recognised"));
        }

        return new Transaction(request.transactionRef(), account, channel, location, type,
                request.amount(), request.timestamp());
    }
}
