package helen.com.sagaorchestratorservice.messaging.consumer.payment;

import helen.com.sagaorchestratorservice.messaging.event.PaymentFailedEvent;
import helen.com.sagaorchestratorservice.saga.orchestrator.OrderSagaOrchestrator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class PaymentFailedConsumer {
    private final OrderSagaOrchestrator orchestrator;

    @RabbitListener(queues = "${saga.queues.payment-failed}")
    public void consume(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent for saga {}", event.getSagaId());
        orchestrator.handlePaymentFailed(event);
    }
}
