package za.co.sbsa.sbsa_fraud.fraud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.co.sbsa.sbsa_fraud.domain.FraudRule;
import za.co.sbsa.sbsa_fraud.domain.TestData;
import za.co.sbsa.sbsa_fraud.domain.Transaction;
import za.co.sbsa.sbsa_fraud.repository.FraudRuleRepository;

/** Unit tests for rule selection and result collection in the fraud engine (FR-04, FR-06). */
class FraudEngineTest {

    private final FraudRuleRepository ruleRepository = mock(FraudRuleRepository.class);
    private final Transaction transaction =
            TestData.transaction(TestData.account(1, "4128728463"), "50.00", Instant.parse("2026-01-15T09:00:00Z"));

    private static FraudRuleEvaluator evaluator(String code, boolean fires) {
        FraudRuleEvaluator evaluator = mock(FraudRuleEvaluator.class);
        when(evaluator.ruleCode()).thenReturn(code);
        when(evaluator.isTriggered(any(), any())).thenReturn(fires);
        return evaluator;
    }

    @Test
    @DisplayName("TC-07: every active rule is evaluated and fired rules are reported")
    void evaluatesActiveRulesAndCollectsFired() {
        FraudRule velocity = TestData.velocityRule();
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(velocity));
        FraudEngine engine = new FraudEngine(ruleRepository, List.of(evaluator(TestData.VELOCITY, true)));

        FraudAssessment assessment = engine.evaluate(transaction);

        assertThat(assessment.rulesEvaluated()).isEqualTo(1);
        assertThat(assessment.firedRules()).containsExactly(velocity);
        assertThat(assessment.fraudSuspected()).isTrue();
    }

    @Test
    @DisplayName("No rule fires: assessment is clean")
    void cleanTransaction() {
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(TestData.velocityRule()));
        FraudEngine engine = new FraudEngine(ruleRepository, List.of(evaluator(TestData.VELOCITY, false)));

        FraudAssessment assessment = engine.evaluate(transaction);

        assertThat(assessment.rulesEvaluated()).isEqualTo(1);
        assertThat(assessment.fraudSuspected()).isFalse();
    }

    @Test
    @DisplayName("TC-14: inactive rules are not evaluated (the repository returns active rules only)")
    void noActiveRulesMeansNothingEvaluated() {
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of());
        FraudEngine engine = new FraudEngine(ruleRepository, List.of(evaluator(TestData.VELOCITY, true)));

        FraudAssessment assessment = engine.evaluate(transaction);

        assertThat(assessment.rulesEvaluated()).isZero();
        assertThat(assessment.fraudSuspected()).isFalse();
    }

    @Test
    @DisplayName("An active rule with no evaluator is skipped, not counted")
    void ruleWithoutEvaluatorIsSkipped() {
        FraudRule unknown = TestData.rule("LOCATION_ANOMALY", 1, BigDecimal.ONE, 1);
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(TestData.velocityRule(), unknown));
        FraudEngine engine = new FraudEngine(ruleRepository, List.of(evaluator(TestData.VELOCITY, false)));

        FraudAssessment assessment = engine.evaluate(transaction);

        assertThat(assessment.rulesEvaluated()).isEqualTo(1);
    }
}
