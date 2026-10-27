package za.co.sbsa.sbsa_fraud.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.sbsa.sbsa_fraud.domain.TransactionType;

/** Finds transaction types by name when a transaction is submitted (FR-02). */
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Integer> {

    Optional<TransactionType> findByTypeName(String typeName);
}
