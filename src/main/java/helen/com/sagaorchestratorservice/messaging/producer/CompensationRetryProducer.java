package helen.com.sagaorchestratorservice.messaging.producer;

import helen.com.sagaorchestratorservice.messaging.command.RetryCompensationCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompensationRetryProducer {
    private final SagaCommandProducer producer;

    @Value("${saga.retry-exchange}")
    private String retryExchange;

    public void scheduleRetry(RetryCompensationCommand command) {
        producer.publish(retryExchange, "compensation.retry", command);
    }
}