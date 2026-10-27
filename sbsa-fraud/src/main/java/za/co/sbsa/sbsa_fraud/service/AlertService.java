package za.co.sbsa.sbsa_fraud.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.sbsa.sbsa_fraud.domain.FraudAlert;
import za.co.sbsa.sbsa_fraud.domain.FraudRule;
import za.co.sbsa.sbsa_fraud.domain.Transaction;
import za.co.sbsa.sbsa_fraud.repository.FraudAlertRepository;

/** Raises fraud alerts automatically, with no human step (FR-08, US-04). */
@Service
@RequiredArgsConstructor
public class AlertService {

    private final FraudAlertRepository fraudAlertRepository;

    /**
     * Saves one OPEN alert for each rule that fired.
     *
     * @param transaction the saved, flagged transaction
     * @param firedRules  the rules that fired; nothing is saved when empty
     * @return the saved alerts
     */
    public List<FraudAlert> raiseAlerts(Transaction transaction, List<FraudRule> firedRules) {
        return firedRules.stream()
                .map(rule -> fraudAlertRepository.save(new FraudAlert(transaction, rule)))
                .toList();
    }
}
