package za.co.sbsa.sbsa_fraud.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.sbsa.sbsa_fraud.domain.FraudRule;

/** Reads fraud rules and their thresholds (FR-06). */
public interface FraudRuleRepository extends JpaRepository<FraudRule, Integer> {

    /** Rules the fraud engine should evaluate; inactive rules are skipped (US-06). */
    List<FraudRule> findByActiveTrue();

    Optional<FraudRule> findByRuleCode(String ruleCode);
}
