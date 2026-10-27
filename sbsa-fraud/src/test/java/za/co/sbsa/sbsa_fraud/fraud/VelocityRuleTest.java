package za.co.sbsa.sbsa_fraud.fraud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.sbsa.sbsa_fraud.domain.Account;
import za.co.sbsa.sbsa_fraud.domain.FraudRule;
import za.co.sbsa.sbsa_fraud.domain.TestData;
import za.co.sbsa.sbsa_fraud.domain.Transaction;
import za.co.sbsa.sbsa_fraud.repository.TransactionRepository;

/** Unit tests for the velocity rule's decision logic (FR-05, US-03). */
@ExtendWith(MockitoExtension.class)
class VelocityRuleTest {

    private static final Instant NOW = Instant.parse("2026-01-15T09:00:00Z");
    private static final BigDecimal CEILING = new BigDecimal("100.00");

    @Mock
    private TransactionRepository transactionRepository;

    private VelocityRule velocityRule;
    private Account account;
    private FraudRule rule;

    @BeforeEach
    void setUp() {
        velocityRule = new VelocityRule(transactionRepository);
        account = TestData.account(1, "4128728463");
        rule = TestData.velocityRule();
    }

    private void earlierMicroTransactions(long count) {
        when(transactionRepository.countMicroTransactionsInWindow(account, CEILING,
                NOW.minus(Duration.ofMinutes(10)), NOW)).thenReturn(count);
    }

    @Test
    @DisplayName("TC-08: 4th micro-transaction in 10 minutes does not fire")
    void fourthMicroTransactionDoesNotFire() {
        earlierMicroTransactions(3);
        Transaction fourth = TestData.transaction(account, "50.00", NOW);

        assertThat(velocityRule.isTriggered(fourth, rule)).isFalse();
    }

    @Test
    @DisplayName("TC-09: 5th micro-transaction in 10 minutes fires")
    void fifthMicroTransactionFires() {
        earlierMicroTransactions(4);
        Transaction fifth = TestData.transaction(account, "50.00", NOW);

        assertThat(velocityRule.isTriggered(fifth, rule)).isTrue();
    }

    @Test
    @DisplayName("TC-10: R100.01 is above the ceiling, never counted and never fires")
    void amountAboveCeilingNeverFires() {
        Transaction large = TestData.transaction(account, "100.01", NOW);

        assertThat(velocityRule.isTriggered(large, rule)).isFalse();
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("Boundary: exactly R100.00 counts as a micro-transaction")
    void amountEqualToCeilingCounts() {
        earlierMicroTransactions(4);
        Transaction atCeiling = TestData.transaction(account, "100.00", NOW);

        assertThat(velocityRule.isTriggered(atCeiling, rule)).isTrue();
    }

    @Test
    @DisplayName("Window is the rolling 10 minutes ending at the transaction's own timestamp (Q4)")
    void queriesRollingWindowEndingAtTransactionTime() {
        earlierMicroTransactions(0);
        velocityRule.isTriggered(TestData.transaction(account, "20.00", NOW), rule);

        verify(transactionRepository).countMicroTransactionsInWindow(account, CEILING,
                Instant.parse("2026-01-15T08:50:00Z"), NOW);
    }

    @Test
    @DisplayName("A rule row missing a threshold is not applied")
    void misconfiguredRuleDoesNotFire() {
        FraudRule broken = TestData.rule(TestData.VELOCITY, null, CEILING, 10);

        assertThat(velocityRule.isTriggered(TestData.transaction(account, "20.00", NOW), broken)).isFalse();
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("TC-13: thresholds come from the rule row, not the code (FR-06)")
    void thresholdsAreReadFromRule() {
        FraudRule stricter = TestData.rule(TestData.VELOCITY, 3, CEILING, 10);
        when(transactionRepository.countMicroTransactionsInWindow(any(), any(), any(), any())).thenReturn(2L);

        assertThat(velocityRule.isTriggered(TestData.transaction(account, "20.00", NOW), stricter)).isTrue();
    }
}
