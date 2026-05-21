package helen.com.sagaorchestratorservice.messaging.consumer;

import helen.com.sagaorchestratorservice.messaging.event.PaymentFailedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentFailedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @SqsListener("payment-failed-queue")
    public void consume(PaymentFailedEvent event) {
        orchestrator.handlePaymentFailed(event);
    }
}
