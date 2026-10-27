package za.co.sbsa.sbsa_fraud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import za.co.sbsa.sbsa_fraud.domain.AlertStatus;
import za.co.sbsa.sbsa_fraud.domain.TestData;
import za.co.sbsa.sbsa_fraud.domain.Transaction;
import za.co.sbsa.sbsa_fraud.repository.DecisionLogRepository;
import za.co.sbsa.sbsa_fraud.repository.FraudAlertRepository;
import za.co.sbsa.sbsa_fraud.repository.TransactionRepository;
import za.co.sbsa.sbsa_fraud.service.TransactionService;
import za.co.sbsa.sbsa_fraud.service.exception.DuplicateTransactionException;
import za.co.sbsa.sbsa_fraud.service.exception.InvalidTransactionException;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionRequest;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionResponse;

/**
 * End-to-end fraud scenarios against the real MySQL database, using the seeded rule
 * and accounts (US-03, US-04, US-05). Proves the velocity query, the index-backed window
 * and the whole processing flow together.
 * <p>
 * Each test runs in a transaction that is rolled back afterwards, so nothing it writes
 * stays in the database. Needs MySQL running with DB_USERNAME and DB_PASSWORD set.
 */
@SpringBootTest
@Transactional
class FraudScenariosIntegrationTest {

    /** Two seeded accounts from changeset 006. */
    private static final String ACCOUNT_A = "4128728463";
    private static final String ACCOUNT_B = "4141227216";

    @Autowired private TransactionService transactionService;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private FraudAlertRepository fraudAlertRepository;
    @Autowired private DecisionLogRepository decisionLogRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    /** A random past moment, so tests never overlap seed data or each other's windows. */
    private Instant base;

    @BeforeEach
    void pickTimeSlot() {
        base = Instant.parse("2024-01-01T00:00:00Z")
                .plus(Duration.ofMinutes(ThreadLocalRandom.current().nextLong(0, 500_000)));
    }

    private TransactionResponse submit(String account, String amount, Duration offset) {
        return transactionService.submit(new TransactionRequest(TestData.uniqueRef(), account,
                new BigDecimal(amount), "ATM", "POS Purchase", null, base.plus(offset)));
    }

    private List<TransactionResponse> microBurst(String account, int count, Duration spacing) {
        List<TransactionResponse> responses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            responses.add(submit(account, "50.00", spacing.multipliedBy(i)));
        }
        return responses;
    }

    @Test
    @DisplayName("TC-08: four micro-transactions within 10 minutes are all approved")
    void fourInWindowApproved() {
        assertThat(microBurst(ACCOUNT_A, 4, Duration.ofMinutes(2)))
                .extracting(TransactionResponse::decision).containsOnly("Success");
    }

    @Test
    @DisplayName("TC-09 / TC-16: the fifth micro-transaction within 10 minutes is flagged and alerted")
    void fifthInWindowFlagged() {
        List<TransactionResponse> responses = microBurst(ACCOUNT_A, 5, Duration.ofMinutes(2));
        TransactionResponse fifth = responses.get(4);

        assertThat(fifth.decision()).isEqualTo("Flagged");
        assertThat(fifth.rulesFired()).containsExactly("VELOCITY_MICRO");

        Transaction saved = transactionRepository.findByTransactionRef(fifth.transactionRef()).orElseThrow();
        assertThat(fraudAlertRepository.findByTransaction(saved))
                .singleElement()
                .satisfies(alert -> assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN));
    }

    @Test
    @DisplayName("TC-10: a transaction of R100.01 is not counted")
    void amountAboveCeilingNotCounted() {
        microBurst(ACCOUNT_A, 3, Duration.ofMinutes(1));
        submit(ACCOUNT_A, "100.01", Duration.ofMinutes(4));

        assertThat(submit(ACCOUNT_A, "50.00", Duration.ofMinutes(5)).decision()).isEqualTo("Success");
    }

    @Test
    @DisplayName("TC-11: five micro-transactions spread over 11 minutes are not flagged")
    void spreadOverElevenMinutesNotFlagged() {
        // 0:00, 2:45, 5:30, 8:15, 11:00 - the first falls outside the last one's 10-minute window
        assertThat(microBurst(ACCOUNT_A, 5, Duration.ofSeconds(165)))
                .extracting(TransactionResponse::decision).containsOnly("Success");
    }

    @Test
    @DisplayName("TC-12: transactions on a different account are not counted")
    void otherAccountNotCounted() {
        microBurst(ACCOUNT_A, 4, Duration.ofMinutes(1));

        assertThat(submit(ACCOUNT_B, "50.00", Duration.ofMinutes(5)).decision()).isEqualTo("Success");
    }

    @Test
    @DisplayName("TC-17: an approved transaction raises no alert")
    void approvedHasNoAlert() {
        TransactionResponse response = submit(ACCOUNT_A, "500.00", Duration.ZERO);
        Transaction saved = transactionRepository.findByTransactionRef(response.transactionRef()).orElseThrow();

        assertThat(fraudAlertRepository.findByTransaction(saved)).isEmpty();
    }

    @Test
    @DisplayName("TC-18: every processed transaction writes exactly one decision log row")
    void oneDecisionLogPerTransaction() {
        long before = decisionLogRepository.count();

        List<TransactionResponse> responses = microBurst(ACCOUNT_A, 5, Duration.ofMinutes(1));

        assertThat(decisionLogRepository.count()).isEqualTo(before + 5);
        Transaction flagged = transactionRepository
                .findByTransactionRef(responses.get(4).transactionRef()).orElseThrow();
        assertThat(decisionLogRepository.findByTransaction(flagged)).isPresent();
    }

    @Test
    @DisplayName("TC-06: the same reference submitted twice is rejected the second time")
    void duplicateRejectedEndToEnd() {
        TransactionRequest request = new TransactionRequest(TestData.uniqueRef(), ACCOUNT_A,
                new BigDecimal("75.00"), "ATM", "POS Purchase", null, base);
        transactionService.submit(request);

        assertThatThrownBy(() -> transactionService.submit(request))
                .isInstanceOf(DuplicateTransactionException.class);
    }

    @Test
    @DisplayName("TC-22: SQL injection in the account number is treated as plain text and rejected")
    void sqlInjectionIsHarmless() {
        long before = transactionRepository.count();

        assertThatThrownBy(() -> transactionService.submit(new TransactionRequest(TestData.uniqueRef(),
                "' OR '1'='1", new BigDecimal("10.00"), "ATM", "POS Purchase", null, base)))
                .isInstanceOf(InvalidTransactionException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_ACCOUNT");
        assertThat(transactionRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("TC-23: the application has no delete for the decision log, and the database blocks changes")
    void decisionLogIsAppendOnly() {
        submit(ACCOUNT_A, "500.00", Duration.ZERO);

        assertThat(Arrays.stream(DecisionLogRepository.class.getMethods()))
                .noneMatch(method -> method.getName().startsWith("delete"));
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE DecisionLog SET ProcessingMs = 0"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM DecisionLog"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }
}
