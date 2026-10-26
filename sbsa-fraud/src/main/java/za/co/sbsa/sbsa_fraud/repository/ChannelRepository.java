package za.co.sbsa.sbsa_fraud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.sbsa.sbsa_fraud.domain.Channel;

import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel,Integer> {
    Optional<Channel> findByName(String Name);
}
