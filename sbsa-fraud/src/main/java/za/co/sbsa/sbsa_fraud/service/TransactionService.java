package za.co.sbsa.sbsa_fraud.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.sbsa.sbsa_fraud.domain.FraudRule;
import za.co.sbsa.sbsa_fraud.domain.Transaction;
import za.co.sbsa.sbsa_fraud.fraud.FraudAssessment;
import za.co.sbsa.sbsa_fraud.fraud.FraudEngine;
import za.co.sbsa.sbsa_fraud.repository.FraudAlertRepository;
import za.co.sbsa.sbsa_fraud.repository.TransactionRepository;
import za.co.sbsa.sbsa_fraud.service.exception.DuplicateTransactionException;
import za.co.sbsa.sbsa_fraud.service.exception.TransactionNotFoundException;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionDetailsResponse;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionDetailsResponse.AlertSummary;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionRequest;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionResponse;

/**
 * Processes transactions: validation, synchronous fraud screening, decision, alerts and
 * audit log, all inside one database transaction (FR-04).
 * <p>
 * If any step fails, nothing is saved: there is never a transaction without its decision
 * log row, or an alert without its transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionValidator transactionValidator;
    private final FraudEngine fraudEngine;
    private final AlertService alertService;
    private final DecisionLogService decisionLogService;
    private final FraudAlertRepository fraudAlertRepository;

    /**
     * Processes one submitted transaction (US-01).
     *
     * @param request the validated-format request
     * @return the decision, rules fired and processing time
     * @throws DuplicateTransactionException if the reference was already processed (FR-03)
     */
    @Transactional
    public TransactionResponse submit(TransactionRequest request) {
        long start = System.nanoTime();

        // Cheapest check first: a retried request should not cost any further lookups.
        if (transactionRepository.existsByTransactionRef(request.transactionRef())) {
            throw new DuplicateTransactionException(request.transactionRef());
        }

        Transaction transaction = transactionValidator.toValidTransaction(request);
        FraudAssessment assessment = fraudEngine.evaluate(transaction);
        decide(transaction, assessment);

        // Saved once, already in its final state: no window in which it reads as PENDING.
        transactionRepository.save(transaction);
        alertService.raiseAlerts(transaction, assessment.firedRules());

        int processingMs = elapsedMillis(start);
        decisionLogService.record(transaction, assessment, processingMs);

        List<String> rulesFired = assessment.firedRules().stream().map(FraudRule::getRuleCode).toList();
        log.info("Transaction {} decided {} in {} ms ({} of {} rules fired)",
                transaction.getTransactionRef(), transaction.getStatus(), processingMs,
                rulesFired.size(), assessment.rulesEvaluated());

        return new TransactionResponse(transaction.getTransactionRef(),
                transaction.getStatus().getDbValue(), rulesFired, processingMs);
    }

    /**
     * Applies the fraud assessment: held if any rule fired, otherwise cleared (FR-07).
     */
    void decide(Transaction transaction, FraudAssessment assessment) {
        if (assessment.fraudSuspected()) {
            transaction.flag();
        } else {
            transaction.approve();
        }
    }

    /**
     * Looks up a transaction and its alerts by reference (US-07).
     *
     * @throws TransactionNotFoundException if the reference is unknown
     */
    @Transactional(readOnly = true)
    public TransactionDetailsResponse findByReference(String transactionRef) {
        Transaction transaction = transactionRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new TransactionNotFoundException(transactionRef));

        List<AlertSummary> alerts = fraudAlertRepository.findByTransaction(transaction).stream()
                .map(alert -> new AlertSummary(alert.getRule().getRuleCode(),
                        alert.getStatus().getDbValue(), alert.getCreatedAt()))
                .toList();

        return new TransactionDetailsResponse(transaction.getTransactionRef(),
                transaction.getAccount().getAccountNumber(), transaction.getAmount(),
                transaction.getTransactionTimestamp(), transaction.getStatus().getDbValue(), alerts);
    }

    private static int elapsedMillis(long startNanos) {
        return (int) ((System.nanoTime() - startNanos) / 1_000_000);
    }
}
