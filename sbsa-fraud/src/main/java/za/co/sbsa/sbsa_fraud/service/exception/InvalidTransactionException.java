package za.co.sbsa.sbsa_fraud.service.exception;

import lombok.Getter;

/**
 * Thrown when a request is well-formed but refers to something that does not exist
 * or is not allowed, e.g. an unknown account (FR-02). Returned as HTTP 400.
 */
@Getter
public class InvalidTransactionException extends RuntimeException {

    /** Machine-readable reason, e.g. "INVALID_ACCOUNT". */
    private final String code;

    public InvalidTransactionException(String code, String message) {
        super(message);
        this.code = code;
    }
}
