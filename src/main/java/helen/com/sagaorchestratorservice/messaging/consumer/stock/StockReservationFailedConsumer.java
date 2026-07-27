package helen.com.sagaorchestratorservice.messaging.consumer.stock;

import helen.com.sagaorchestratorservice.messaging.event.StockReservationFailedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class StockReservationFailedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @SqsListener("${saga.queues.stock-failed}")
    public void consume(StockReservationFailedEvent event) {
        log.info("Received StockReservationFailedEvent for saga {}", event.getSagaId());
        orchestrator.handleStockFailed(event);
    }
}
