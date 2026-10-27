package za.co.sbsa.sbsa_fraud.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.sbsa.sbsa_fraud.domain.Account;
import za.co.sbsa.sbsa_fraud.domain.Transaction;

/** Stores and looks up transactions. */
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    /** Look-up by reference (FR-11). */
    Optional<Transaction> findByTransactionRef(String transactionRef);

    /** Duplicate check before saving (FR-03). */
    boolean existsByTransactionRef(String transactionRef);

    /**
     * Counts an account's stored transactions of at most {@code ceiling} in the window
     * after {@code from} up to and including {@code to} (FR-05).
     * <p>
     * Served by the index on (AccountID, TransactionTimestamp) from changeset 002-2,
     * so it stays fast as the table grows (NFR-01).
     */
    @Query("""
            select count(t) from Transaction t
            where t.account = :account
              and t.amount <= :ceiling
              and t.transactionTimestamp > :from
              and t.transactionTimestamp <= :to
            """)
    long countMicroTransactionsInWindow(@Param("account") Account account,
                                        @Param("ceiling") BigDecimal ceiling,
                                        @Param("from") Instant from,
                                        @Param("to") Instant to);
}