package helen.com.sagaorchestratorservice.messaging.consumer.order;

import helen.com.sagaorchestratorservice.messaging.event.OrderConfirmedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConfirmedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @SqsListener("${saga.queues.order-confirmed}")
    public void consume(OrderConfirmedEvent event) {
    log.info("Received OrderConfirmedEvent for saga {}", event.getSagaId());
    orchestrator.handleOrderConfirmed(event);
    }
}
