package za.co.sbsa.sbsa_fraud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.sbsa.sbsa_fraud.domain.FraudRule;
import za.co.sbsa.sbsa_fraud.domain.TestData;
import za.co.sbsa.sbsa_fraud.domain.Transaction;
import za.co.sbsa.sbsa_fraud.domain.TransactionStatus;
import za.co.sbsa.sbsa_fraud.fraud.FraudAssessment;
import za.co.sbsa.sbsa_fraud.fraud.FraudEngine;
import za.co.sbsa.sbsa_fraud.repository.FraudAlertRepository;
import za.co.sbsa.sbsa_fraud.repository.TransactionRepository;
import za.co.sbsa.sbsa_fraud.service.exception.DuplicateTransactionException;
import za.co.sbsa.sbsa_fraud.service.exception.TransactionNotFoundException;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionRequest;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionResponse;

/** Unit tests for the processing flow and decision (FR-03, FR-07, FR-10). */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Instant TIME = Instant.parse("2026-01-15T09:00:00Z");

    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionValidator transactionValidator;
    @Mock private FraudEngine fraudEngine;
    @Mock private AlertService alertService;
    @Mock private DecisionLogService decisionLogService;
    @Mock private FraudAlertRepository fraudAlertRepository;

    @InjectMocks
    private TransactionService transactionService;

    private final TransactionRequest request = new TransactionRequest(
            "T-1", "4128728463", new BigDecimal("50.00"), "ATM", "POS Purchase", null, TIME);

    private Transaction pendingTransaction() {
        Transaction transaction = TestData.transaction(TestData.account(1, "4128728463"), "50.00", TIME);
        when(transactionValidator.toValidTransaction(request)).thenReturn(transaction);
        return transaction;
    }

    @Test
    @DisplayName("TC-06: a reference that was already processed is rejected and nothing is saved")
    void duplicateReferenceRejected() {
        when(transactionRepository.existsByTransactionRef("T-1")).thenReturn(true);

        assertThatThrownBy(() -> transactionService.submit(request))
                .isInstanceOf(DuplicateTransactionException.class);
        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(transactionValidator, fraudEngine, alertService, decisionLogService);
    }

    @Test
    @DisplayName("TC-15: a fired rule flags the transaction, raises an alert and is logged")
    void firedRuleFlagsTransaction() {
        Transaction transaction = pendingTransaction();
        FraudRule velocity = TestData.velocityRule();
        FraudAssessment assessment = new FraudAssessment(1, List.of(velocity));
        when(fraudEngine.evaluate(transaction)).thenReturn(assessment);

        TransactionResponse response = transactionService.submit(request);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.FLAGGED);
        assertThat(response.decision()).isEqualTo("Flagged");
        assertThat(response.rulesFired()).containsExactly(TestData.VELOCITY);
        verify(transactionRepository).save(transaction);
        verify(alertService).raiseAlerts(transaction, List.of(velocity));
        verify(decisionLogService).record(eq(transaction), eq(assessment), anyInt());
    }

    @Test
    @DisplayName("TC-15: no rule fired, the transaction is approved")
    void cleanTransactionApproved() {
        Transaction transaction = pendingTransaction();
        FraudAssessment assessment = new FraudAssessment(1, List.of());
        when(fraudEngine.evaluate(transaction)).thenReturn(assessment);

        TransactionResponse response = transactionService.submit(request);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(response.decision()).isEqualTo("Success");
        assertThat(response.rulesFired()).isEmpty();
        assertThat(response.processingMs()).isGreaterThanOrEqualTo(0);
        verify(decisionLogService).record(eq(transaction), eq(assessment), anyInt());
    }

    @Test
    @DisplayName("A transaction can be decided only once")
    void decisionIsFinal() {
        Transaction transaction = TestData.transaction(TestData.account(1, "4128728463"), "50.00", TIME);
        transaction.approve();

        assertThatThrownBy(transaction::flag).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("TC-20: looking up an unknown reference throws TransactionNotFoundException")
    void unknownReference() {
        when(transactionRepository.findByTransactionRef("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findByReference("NOPE"))
                .isInstanceOf(TransactionNotFoundException.class);
    }
}
