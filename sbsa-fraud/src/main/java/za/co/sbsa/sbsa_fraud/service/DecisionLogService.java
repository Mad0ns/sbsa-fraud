package za.co.sbsa.sbsa_fraud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.sbsa.sbsa_fraud.domain.Decision;
import za.co.sbsa.sbsa_fraud.domain.DecisionLog;
import za.co.sbsa.sbsa_fraud.domain.Transaction;
import za.co.sbsa.sbsa_fraud.domain.TransactionStatus;
import za.co.sbsa.sbsa_fraud.fraud.FraudAssessment;
import za.co.sbsa.sbsa_fraud.repository.DecisionLogRepository;

/** Writes the append-only audit record of every fraud decision (FR-09, US-05). */
@Service
@RequiredArgsConstructor
public class DecisionLogService {

    private final DecisionLogRepository decisionLogRepository;

    /**
     * Records how a transaction was decided.
     *
     * @param transaction  the saved transaction, already approved or flagged
     * @param assessment   what the fraud engine found
     * @param processingMs time taken, in milliseconds
     * @return the saved log row
     */
    public DecisionLog record(Transaction transaction, FraudAssessment assessment, int processingMs) {
        Decision decision = transaction.getStatus() == TransactionStatus.FLAGGED
                ? Decision.FLAGGED
                : Decision.SUCCESS;
        return decisionLogRepository.save(new DecisionLog(transaction,
                (short) assessment.rulesEvaluated(),
                (short) assessment.firedRules().size(),
                decision,
                processingMs));
    }
}
