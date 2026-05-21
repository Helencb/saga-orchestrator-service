package helen.com.sagaorchestratorservice.messaging.consumer;

import helen.com.sagaorchestratorservice.messaging.event.OrderCreatedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @SqsListener("order-created-queue")
    public void consume(OrderCreatedEvent event) {
        orchestrator.handleOrderCreated(event);
    }
}
