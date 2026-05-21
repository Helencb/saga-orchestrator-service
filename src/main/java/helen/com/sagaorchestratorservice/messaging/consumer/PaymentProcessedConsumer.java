package helen.com.sagaorchestratorservice.messaging.consumer;

import helen.com.sagaorchestratorservice.messaging.event.PaymentProcessedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentProcessedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @SqsListener("payment-processed-queue")
    public void consume(PaymentProcessedEvent event) {
        orchestrator.handlePaymentProcessed(event);
    }
}
