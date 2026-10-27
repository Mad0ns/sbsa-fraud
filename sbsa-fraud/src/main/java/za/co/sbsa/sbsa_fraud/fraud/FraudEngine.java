package za.co.sbsa.sbsa_fraud.fraud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.co.sbsa.sbsa_fraud.domain.FraudRule;
import za.co.sbsa.sbsa_fraud.domain.Transaction;
import za.co.sbsa.sbsa_fraud.repository.FraudRuleRepository;

/**
 * Runs every active fraud rule against a transaction before it is saved (FR-04).
 * <p>
 * Which rules run is decided by the FraudRule table (FR-06): an inactive rule is skipped,
 * and an active rule with no matching {@link FraudRuleEvaluator} is logged and skipped
 * rather than failing every transaction.
 */
@Slf4j
@Component
public class FraudEngine {

    private final FraudRuleRepository fraudRuleRepository;
    private final Map<String, FraudRuleEvaluator> evaluatorsByCode;

    /**
     * @param fraudRuleRepository source of the active rules
     * @param evaluators          every {@link FraudRuleEvaluator} bean; Spring supplies them all
     */
    public FraudEngine(FraudRuleRepository fraudRuleRepository, List<FraudRuleEvaluator> evaluators) {
        this.fraudRuleRepository = fraudRuleRepository;
        this.evaluatorsByCode = evaluators.stream()
                .collect(Collectors.toMap(FraudRuleEvaluator::ruleCode, Function.identity()));
    }

    /**
     * Screens a transaction against all active rules.
     *
     * @param transaction the incoming, not yet saved transaction
     * @return how many rules were checked and which fired
     */
    public FraudAssessment evaluate(Transaction transaction) {
        int evaluated = 0;
        List<FraudRule> fired = new ArrayList<>();

        for (FraudRule rule : fraudRuleRepository.findByActiveTrue()) {
            FraudRuleEvaluator evaluator = evaluatorsByCode.get(rule.getRuleCode());
            if (evaluator == null) {
                log.warn("Active fraud rule {} has no evaluator and was skipped", rule.getRuleCode());
                continue;
            }
            evaluated++;
            if (evaluator.isTriggered(transaction, rule)) {
                fired.add(rule);
            }
        }
        return new FraudAssessment(evaluated, fired);
    }
}
