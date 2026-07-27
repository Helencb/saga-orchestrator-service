package helen.com.sagaorchestratorservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "saga")
public class SagaProperties {

    private Integer maxRetry = 3;
    private Integer timeoutSeconds = 300;
    private String orderTopic;
    private String paymentTopic;
    private String inventoryTopic;
    private String retryTopic;
    private Map<String, String> queues;
}