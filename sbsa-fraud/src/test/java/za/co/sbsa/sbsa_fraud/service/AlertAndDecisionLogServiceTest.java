package za.co.sbsa.sbsa_fraud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import za.co.sbsa.sbsa_fraud.domain.AlertStatus;
import za.co.sbsa.sbsa_fraud.domain.Decision;
import za.co.sbsa.sbsa_fraud.domain.DecisionLog;
import za.co.sbsa.sbsa_fraud.domain.FraudAlert;
import za.co.sbsa.sbsa_fraud.domain.FraudRule;
import za.co.sbsa.sbsa_fraud.domain.TestData;
import za.co.sbsa.sbsa_fraud.domain.Transaction;
import za.co.sbsa.sbsa_fraud.fraud.FraudAssessment;
import za.co.sbsa.sbsa_fraud.repository.DecisionLogRepository;
import za.co.sbsa.sbsa_fraud.repository.FraudAlertRepository;

/** Unit tests for automatic alerts (FR-08) and the decision log (FR-09). */
class AlertAndDecisionLogServiceTest {

    private final Transaction transaction = TestData.transaction(
            TestData.account(1, "4128728463"), "50.00", Instant.parse("2026-01-15T09:00:00Z"));

    @Test
    @DisplayName("TC-16: one OPEN alert is saved per fired rule")
    void alertPerFiredRule() {
        FraudAlertRepository repository = mock(FraudAlertRepository.class);
        when(repository.save(any(FraudAlert.class))).thenAnswer(call -> call.getArgument(0));
        FraudRule velocity = TestData.velocityRule();

        List<FraudAlert> alerts = new AlertService(repository).raiseAlerts(transaction, List.of(velocity));

        assertThat(alerts).singleElement().satisfies(alert -> {
            assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
            assertThat(alert.getRule()).isSameAs(velocity);
            assertThat(alert.getTransaction()).isSameAs(transaction);
        });
    }

    @Test
    @DisplayName("TC-17: a clean transaction raises no alert")
    void noAlertWhenNothingFired() {
        FraudAlertRepository repository = mock(FraudAlertRepository.class);

        assertThat(new AlertService(repository).raiseAlerts(transaction, List.of())).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("TC-18: the decision log records the outcome, rule counts and timing")
    void decisionLogRecordsOutcome() {
        DecisionLogRepository repository = mock(DecisionLogRepository.class);
        when(repository.save(any(DecisionLog.class))).thenAnswer(call -> call.getArgument(0));
        transaction.flag();

        new DecisionLogService(repository).record(transaction,
                new FraudAssessment(1, List.of(TestData.velocityRule())), 42);

        ArgumentCaptor<DecisionLog> saved = ArgumentCaptor.forClass(DecisionLog.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getDecision()).isEqualTo(Decision.FLAGGED);
        assertThat(saved.getValue().getRulesEvaluated()).isEqualTo((short) 1);
        assertThat(saved.getValue().getRulesFired()).isEqualTo((short) 1);
        assertThat(saved.getValue().getProcessingMs()).isEqualTo(42);
    }
}
