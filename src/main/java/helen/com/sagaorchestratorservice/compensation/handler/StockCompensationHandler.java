package helen.com.sagaorchestratorservice.compensation.handler;

import helen.com.sagaorchestratorservice.messaging.command.ReleaseStockCommand;
import helen.com.sagaorchestratorservice.messaging.producer.InventoryCommandProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockCompensationHandler implements CompensationHandler {

    private final InventoryCommandProducer producer;

    @Override
    public void compensate(UUID sagaId, UUID orderId, String correlationId) {
        log.info("Executing STOCK compensation for saga {}", sagaId);

        ReleaseStockCommand command = ReleaseStockCommand.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .correlationId(correlationId)
                .build();
        producer.releaseStock(command);
    }

    @Override
    public Integer getExecutionOrder() {
        return 1;
    }

    @Override
    public String getHandlerName() {
        return "STOCK_COMPENSATION";
    }
}
