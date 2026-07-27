package helen.com.sagaorchestratorservice.messaging.consumer.stock;

import helen.com.sagaorchestratorservice.messaging.event.StockReservedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StockReservedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @SqsListener("${saga.queues.stock-reserved}")
    public void consume(StockReservedEvent event) {
        log.info("Received StockReservedEvent for saga {}", event.getSagaId());
        orchestrator.handleStockReserved(event);
    }
}
