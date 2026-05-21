package helen.com.sagaorchestratorservice.messaging.consumer;

import helen.com.sagaorchestratorservice.messaging.event.StockReservedEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockReservedConsumer {
    @SqsListener("stock-reserved-queue")
    public void consume(StockReservedEvent event) {
        log.info("STOCK_RESERVED event received: {}", event.getSagaId());
    }
}
