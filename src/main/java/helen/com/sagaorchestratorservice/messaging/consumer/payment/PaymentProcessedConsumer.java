package helen.com.sagaorchestratorservice.messaging.consumer.payment;

import helen.com.sagaorchestratorservice.messaging.event.PaymentProcessedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class PaymentProcessedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @SqsListener("${saga.queues.payment-processed}")
    public void consume(PaymentProcessedEvent event) {
        log.info("Received PaymentProcessedEvent for saga {}", event.getSagaId());
        orchestrator.handlePaymentProcessed(event);
    }
}
