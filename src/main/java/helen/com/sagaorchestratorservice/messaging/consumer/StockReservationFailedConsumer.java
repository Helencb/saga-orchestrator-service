package helen.com.sagaorchestratorservice.messaging.consumer;

import helen.com.sagaorchestratorservice.messaging.event.StockReservationFailedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class StockReservationFailedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @SqsListener("stock-failed-queue")
    public void consume(StockReservationFailedEvent event) {
        orchestrator.handleStockFailed(event);
    }
}
