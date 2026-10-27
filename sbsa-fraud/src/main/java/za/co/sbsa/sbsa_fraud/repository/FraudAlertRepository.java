package za.co.sbsa.sbsa_fraud.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.sbsa.sbsa_fraud.domain.FraudAlert;
import za.co.sbsa.sbsa_fraud.domain.Transaction;

/** Stores fraud alerts and lists them for a transaction (FR-08, FR-11). */
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findByTransaction(Transaction transaction);
}
