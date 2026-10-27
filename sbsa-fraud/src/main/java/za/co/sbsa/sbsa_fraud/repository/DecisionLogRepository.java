package za.co.sbsa.sbsa_fraud.repository;

import java.util.Optional;
import org.springframework.data.repository.Repository;
import za.co.sbsa.sbsa_fraud.domain.DecisionLog;
import za.co.sbsa.sbsa_fraud.domain.Transaction;

/**
 * Append-only access to the decision log (FR-09, NFR-03).
 * <p>
 * Extends the bare {@link Repository} instead of {@code JpaRepository}, so only the
 * methods declared here exist: there is no delete, and no code can call one.
 */
public interface DecisionLogRepository extends Repository<DecisionLog, Long> {

    <S extends DecisionLog> S save(S decisionLog);

    Optional<DecisionLog> findByTransaction(Transaction transaction);

    long count();
}
