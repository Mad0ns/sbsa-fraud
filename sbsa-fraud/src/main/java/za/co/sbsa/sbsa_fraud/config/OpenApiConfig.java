package za.co.sbsa.sbsa_fraud.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Title and description shown at the top of the Swagger UI page.
 * Springdoc builds the endpoint list itself from the controllers.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sbsaFraudOpenApi() {
        return new OpenAPI().info(new Info()
                .title("SBSA Fraud Monitoring API")
                .version("1.1.0")
                .description("Submit a transaction and get the fraud decision (Approved or Flagged) "
                        + "in the same response. Flagged transactions raise an alert and every "
                        + "decision is written to the append-only decision log."));
    }
}