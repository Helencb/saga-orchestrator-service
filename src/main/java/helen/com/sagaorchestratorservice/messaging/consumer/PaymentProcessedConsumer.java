package helen.com.sagaorchestratorservice.messaging.consumer;

import helen.com.sagaorchestratorservice.messaging.event.PaymentProcessedEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentProcessedConsumer {
    @SqsListener("payment-processed-queue")
    public void consume(PaymentProcessedEvent event) {
        log.info("PAYMENT_PROCESSED event received: {}", event.getSagaId());
    }
}
