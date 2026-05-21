package helen.com.sagaorchestratorservice.messaging.producer;

import helen.com.sagaorchestratorservice.messaging.command.ReleaseStockCommand;
import helen.com.sagaorchestratorservice.messaging.command.ReserveStockCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryCommandProducer {
    private final SagaCommandProducer producer;

    @Value("${saga.inventory-topic}")
    private String inventoryTopic;

    public void reserveStock(ReserveStockCommand command) {
        producer.publish(inventoryTopic, command);
    }

    public void releaseStock(ReleaseStockCommand command) {
        producer.publish(inventoryTopic, command);
    }
}
