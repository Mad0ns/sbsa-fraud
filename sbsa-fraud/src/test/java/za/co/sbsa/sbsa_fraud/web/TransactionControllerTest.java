package za.co.sbsa.sbsa_fraud.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import za.co.sbsa.sbsa_fraud.service.TransactionService;
import za.co.sbsa.sbsa_fraud.service.exception.DuplicateTransactionException;
import za.co.sbsa.sbsa_fraud.service.exception.InvalidTransactionException;
import za.co.sbsa.sbsa_fraud.service.exception.TransactionNotFoundException;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionDetailsResponse;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionResponse;

/**
 * Tests the HTTP contract: status codes, Location header, request validation and the
 * {code, message} error format (FR-01, FR-02, FR-10, NFR-05). The service is mocked.
 */
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionController(transactionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static String body(String accountNumber, String amount) {
        return """
                {"transactionRef":"T-1","accountNumber":"%s","amount":%s,"channel":"ATM",
                 "transactionType":"POS Purchase","city":"Durban","timestamp":"2026-01-15T09:00:00Z"}
                """.formatted(accountNumber, amount);
    }

    @Test
    @DisplayName("TC-01: valid request returns 201, a Location header and the decision")
    void validSubmission() throws Exception {
        when(transactionService.submit(any()))
                .thenReturn(new TransactionResponse("T-1", "Success", List.of(), 12));

        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(body("4128728463", "50.00")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/transactions/T-1"))
                .andExpect(jsonPath("$.decision").value("Success"))
                .andExpect(jsonPath("$.processingMs").value(12));
    }

    @Test
    @DisplayName("TC-03: amount of zero fails validation with 400 before the service is called")
    void zeroAmountRejected() throws Exception {
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(body("4128728463", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verify(transactionService, never()).submit(any());
    }

    @Test
    @DisplayName("TC-05: missing timestamp fails validation with 400")
    void missingTimestampRejected() throws Exception {
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionRef":"T-1","accountNumber":"4128728463","amount":50,
                                 "channel":"ATM","transactionType":"POS Purchase"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("timestamp")));
    }

    @Test
    @DisplayName("TC-02: business rejection from the service becomes 400 with its code")
    void unknownAccountRejected() throws Exception {
        when(transactionService.submit(any()))
                .thenThrow(new InvalidTransactionException("INVALID_ACCOUNT", "Account 1 does not exist"));

        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(body("1", "50.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ACCOUNT"));
    }

    @Test
    @DisplayName("TC-06: duplicate reference returns 409")
    void duplicateReturnsConflict() throws Exception {
        when(transactionService.submit(any())).thenThrow(new DuplicateTransactionException("T-1"));

        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(body("4128728463", "50.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_TRANSACTION"));
    }

    @Test
    @DisplayName("TC-24: malformed JSON returns 400 {code, message} with no stack trace")
    void malformedJsonRejected() throws Exception {
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionRef\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    @DisplayName("TC-24: unexpected errors return 500 INTERNAL_ERROR without internals")
    void unexpectedErrorHidden() throws Exception {
        when(transactionService.submit(any())).thenThrow(new IllegalStateException("simulated database failure"));

        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(body("4128728463", "50.00")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("TC-19: known reference returns 200 with status and alerts")
    void lookupFound() throws Exception {
        when(transactionService.findByReference("T-1")).thenReturn(new TransactionDetailsResponse(
                "T-1", "4128728463", new BigDecimal("50.00"), Instant.parse("2026-01-15T09:00:00Z"),
                "Flagged", List.of(new TransactionDetailsResponse.AlertSummary(
                        "VELOCITY_MICRO", "Open", Instant.parse("2026-01-15T09:00:01Z")))));

        mockMvc.perform(get("/api/transactions/T-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Flagged"))
                .andExpect(jsonPath("$.alerts[0].ruleCode").value("VELOCITY_MICRO"));
    }

    @Test
    @DisplayName("TC-20: unknown reference returns 404")
    void lookupNotFound() throws Exception {
        when(transactionService.findByReference("NOPE")).thenThrow(new TransactionNotFoundException("NOPE"));

        mockMvc.perform(get("/api/transactions/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
    }
}
