package helen.com.sagaorchestratorservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "saga")
public class SagaProperties {
    private Integer maxRetry;
    private Integer timeoutSeconds;
    private String orderTopic;
    private String paymentTopic;
    private String inventoryTopic;
}
