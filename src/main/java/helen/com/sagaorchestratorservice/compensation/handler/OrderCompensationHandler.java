package helen.com.sagaorchestratorservice.compensation.handler;

import helen.com.sagaorchestratorservice.messaging.command.CancelOrderCommand;
import helen.com.sagaorchestratorservice.messaging.producer.OrderCommandProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompensationHandler implements CompensationHandler{
    private final OrderCommandProducer producer;

    @Override
    public void compensate(UUID sagaId, UUID orderId, String correlationId) {
        log.info("Executing ORDER compensation for saga {}", sagaId);

        CancelOrderCommand command = CancelOrderCommand.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .correlationId(correlationId)
                .build();
        producer.cancelOrder(command);
    }

    @Override
    public Integer getExecutionOrder() {
        return 3;
    }

    @Override
    public String getHandlerName() {
        return "ORDER_COMPENSATION";
    }
}
