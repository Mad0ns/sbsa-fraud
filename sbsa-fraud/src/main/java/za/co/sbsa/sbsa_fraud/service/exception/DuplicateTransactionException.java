package za.co.sbsa.sbsa_fraud.service.exception;

/**
 * Thrown when a transaction reference has already been processed (FR-03).
 * Returned as HTTP 409, so a channel that retries a request never doubles a payment.
 */
public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String transactionRef) {
        super("Transaction " + transactionRef + " has already been processed");
    }
}
