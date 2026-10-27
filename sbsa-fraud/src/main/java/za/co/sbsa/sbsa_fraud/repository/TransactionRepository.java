package za.co.sbsa.sbsa_fraud.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.sbsa.sbsa_fraud.domain.Transaction;

/**
 * Stores and looks up transactions.
 * The velocity-window count (FR-05) is added with the velocity rule.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    /** Look-up by reference (FR-11). */
    Optional<Transaction> findByTransactionRef(String transactionRef);

    /** Duplicate check before saving (FR-03). */
    boolean existsByTransactionRef(String transactionRef);
}
