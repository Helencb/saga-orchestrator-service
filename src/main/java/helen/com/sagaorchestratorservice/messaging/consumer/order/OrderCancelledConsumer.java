package helen.com.sagaorchestratorservice.messaging.consumer.order;

import helen.com.sagaorchestratorservice.messaging.event.OrderCancelledEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @SqsListener("${saga.queues.order-cancelled}")
    public void consume(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for saga {}", event.getSagaId());
        orchestrator.handleOrderCancelled(event);
    }
}
