package helen.com.sagaorchestratorservice.messaging.consumer;

import helen.com.sagaorchestratorservice.messaging.event.StockReservedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class StockReservedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @SqsListener("stock-reserved-queue")
    public void consume(StockReservedEvent event) {
        orchestrator.handleStockReserved(event);
    }
}
