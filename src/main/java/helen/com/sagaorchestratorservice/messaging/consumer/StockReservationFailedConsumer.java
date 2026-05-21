package helen.com.sagaorchestratorservice.messaging.consumer;

import helen.com.sagaorchestratorservice.messaging.event.StockReservationFailedEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockReservationFailedConsumer {
    @SqsListener("stock-failed-queue")
    public void consume(StockReservationFailedEvent event) {
        log.info("STOCK_FAILED event received: {}", event.getSagaId());
    }
}
