package helen.com.sagaorchestratorservice.messaging.producer;

import helen.com.sagaorchestratorservice.messaging.command.RetryCompensationCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompensationRetryProducer {
    private final SagaCommandProducer producer;

    @Value("${saga.retry-topic}")
    private String retryTopic;

    public void scheduleRetry(RetryCompensationCommand command) {
        producer.publish(retryTopic, command);
    }
}