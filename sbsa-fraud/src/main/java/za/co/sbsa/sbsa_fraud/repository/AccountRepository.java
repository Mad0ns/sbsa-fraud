package za.co.sbsa.sbsa_fraud.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.sbsa.sbsa_fraud.domain.Account;

/** Finds accounts by account number; an unknown number is rejected (FR-02). */
public interface AccountRepository extends JpaRepository<Account, Integer> {

    Optional<Account> findByAccountNumber(String accountNumber);
}
