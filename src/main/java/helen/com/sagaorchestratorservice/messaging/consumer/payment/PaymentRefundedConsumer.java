package helen.com.sagaorchestratorservice.messaging.consumer.payment;

import helen.com.sagaorchestratorservice.messaging.event.PaymentRefundedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @RabbitListener(queues = "${saga.queues.payment-refunded}")
    public void consume(PaymentRefundedEvent event) {
        log.info("Received PaymentRefundedEvent for saga {}", event.getSagaId());
        orchestrator.handlePaymentRefunded(event);
    }
}
