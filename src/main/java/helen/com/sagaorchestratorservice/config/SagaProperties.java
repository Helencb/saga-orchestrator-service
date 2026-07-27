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
    private String orderExchange;
    private String paymentExchange;
    private String inventoryExchange;
    private String retryExchange;
    private Map<String, String> queues;
}