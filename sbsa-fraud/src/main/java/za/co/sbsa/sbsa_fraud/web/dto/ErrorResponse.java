package za.co.sbsa.sbsa_fraud.web.dto;

/**
 * Every error the API returns has this shape (NFR-05): a stable code a program can
 * check, and a message a person can read. Stack traces are never included.
 *
 * @param code    machine-readable error code, e.g. "INVALID_ACCOUNT"
 * @param message human-readable explanation
 */
public record ErrorResponse(String code, String message) {
}
