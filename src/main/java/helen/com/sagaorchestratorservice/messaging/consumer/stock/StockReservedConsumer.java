package helen.com.sagaorchestratorservice.messaging.consumer.stock;

import helen.com.sagaorchestratorservice.messaging.event.StockReservedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StockReservedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @RabbitListener(queues = "${saga.queues.stock-reserved}")
    public void consume(StockReservedEvent event) {
        log.info("Received StockReservedEvent for saga {}", event.getSagaId());
        orchestrator.handleStockReserved(event);
    }
}
