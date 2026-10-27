package za.co.sbsa.sbsa_fraud.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the clock the application uses for "now".
 * <p>
 * Code asks for {@code Instant.now(clock)} instead of {@code Instant.now()}, so a test
 * can supply a fixed clock and check time-based rules (future timestamps, the velocity
 * window) with exact, repeatable times.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
