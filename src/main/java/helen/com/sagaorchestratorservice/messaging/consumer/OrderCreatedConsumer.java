package helen.com.sagaorchestratorservice.messaging.consumer;

import helen.com.sagaorchestratorservice.messaging.event.OrderCreatedEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderCreatedConsumer {

    @SqsListener("order-created-queue")
    public void consume(OrderCreatedEvent event) {
        log.info("ORDER_CREATED event received: {}", event.getSagaId());
    }
}
