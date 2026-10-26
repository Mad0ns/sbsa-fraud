package za.co.sbsa.sbsa_fraud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.sbsa.sbsa_fraud.domain.Location;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location,Integer> {

    Optional<Location> findByCity(String city);
}
