package za.co.sbsa.sbsa_fraud.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import za.co.sbsa.sbsa_fraud.service.exception.DuplicateTransactionException;
import za.co.sbsa.sbsa_fraud.service.exception.InvalidTransactionException;
import za.co.sbsa.sbsa_fraud.service.exception.TransactionNotFoundException;
import za.co.sbsa.sbsa_fraud.web.dto.ErrorResponse;

/**
 * Turns exceptions into {@link ErrorResponse} JSON with the right HTTP status (NFR-05).
 * Controllers and services just throw; this class decides what the caller sees.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** A field failed its annotation check on {@code TransactionRequest}: 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .findFirst()
                .orElse("Request is invalid");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }

    /** The body was not valid JSON, or a value had the wrong type: 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is not valid JSON or has a value of the wrong type");
    }

    /** Unknown account, channel, type or city, or a future timestamp: 400. */
    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(InvalidTransactionException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage());
    }

    /** Reference already processed: 409. */
    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateTransactionException ex) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_TRANSACTION", ex.getMessage());
    }

    /** No transaction with that reference: 404 (FR-11). */
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TransactionNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", ex.getMessage());
    }

    /**
     * The database rejected a write, e.g. two identical references arriving at the same
     * moment both passed the duplicate check. The UNIQUE constraint is the final guard: 409.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with existing data");
    }

    /** Anything unexpected: logged in full here, but the caller sees no internals: 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}
