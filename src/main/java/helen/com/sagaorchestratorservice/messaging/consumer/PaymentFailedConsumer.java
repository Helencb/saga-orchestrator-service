package helen.com.sagaorchestratorservice.messaging.consumer;

import helen.com.sagaorchestratorservice.messaging.event.PaymentFailedEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentFailedConsumer {
    @SqsListener("payment-failed-queue")
    public void consume(PaymentFailedEvent event) {
        log.info("PAYMENT_FAILED event received: {}", event.getSagaId());
    }
}
