package za.co.sbsa.sbsa_fraud.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.sbsa.sbsa_fraud.domain.Channel;

/** Finds channels by name when a transaction is submitted (FR-02). */
public interface ChannelRepository extends JpaRepository<Channel, Integer> {

    Optional<Channel> findByName(String name);
}
