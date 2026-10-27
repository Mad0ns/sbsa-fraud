package za.co.sbsa.sbsa_fraud.service.exception;

/** Thrown when no transaction has the requested reference (FR-11). Returned as HTTP 404. */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String transactionRef) {
        super("Transaction " + transactionRef + " was not found");
    }
}
