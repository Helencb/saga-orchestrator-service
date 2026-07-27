package helen.com.sagaorchestratorservice.messaging.consumer.stock;

import helen.com.sagaorchestratorservice.messaging.event.StockReleasedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockReleasedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @RabbitListener(queues = "${saga.queues.stock-released}")
    public void consume(StockReleasedEvent event) {
        log.info("Received StockReleasedEvent for saga {}", event.getSagaId());
        orchestrator.handleStockReleased(event);
    }
}
