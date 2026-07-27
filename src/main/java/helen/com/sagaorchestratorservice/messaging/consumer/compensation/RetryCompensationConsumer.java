package helen.com.sagaorchestratorservice.messaging.consumer.compensation;

import helen.com.sagaorchestratorservice.compensation.CompensationService;
import helen.com.sagaorchestratorservice.messaging.command.RetryCompensationCommand;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryCompensationConsumer {
    private final CompensationService compensationService;

    @SqsListener("${saga.queues.retry-compensation}")
    public void consume(RetryCompensationCommand command) {
        log.info("Received retry request for handler {} on saga {} (attempt {})",
                command.getHandlerName(), command.getSagaId(), command.getRetryCount());
        compensationService.retryCompensation(command);
    }
}