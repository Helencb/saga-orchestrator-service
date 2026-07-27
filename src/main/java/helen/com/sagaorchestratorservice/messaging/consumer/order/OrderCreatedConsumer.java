package helen.com.sagaorchestratorservice.messaging.consumer.order;

import helen.com.sagaorchestratorservice.messaging.event.OrderCreatedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @SqsListener("${saga.queues.order-created}")
    public void consume(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for saga {}", event.getSagaId());
        orchestrator.handleOrderCreated(event);
    }
}
