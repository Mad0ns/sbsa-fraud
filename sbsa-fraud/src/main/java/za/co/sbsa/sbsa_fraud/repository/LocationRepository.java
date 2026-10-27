package za.co.sbsa.sbsa_fraud.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.sbsa.sbsa_fraud.domain.Location;

/** Finds locations by city name when a transaction is submitted (FR-02). */
public interface LocationRepository extends JpaRepository<Location, Integer> {

    Optional<Location> findByCity(String city);
}
