package helen.com.sagaorchestratorservice.messaging.producer;

import helen.com.sagaorchestratorservice.messaging.command.CancelOrderCommand;
import helen.com.sagaorchestratorservice.messaging.command.ConfirmOrderCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCommandProducer {
    private final SagaCommandProducer producer;

    @Value("${saga.order-exchange}")
    private String orderExchange;

    public void confirmOrder(ConfirmOrderCommand command) {
        producer.publish(orderExchange, "order.confirm", command);
    }

    public void cancelOrder(CancelOrderCommand command) {
        producer.publish(orderExchange, "order.cancel", command);
    }
}
