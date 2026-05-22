package helen.com.sagaorchestratorservice.statemachine;

import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaEventType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SagaTransitionRegistry {
    private final Map<SagaTransitionKey, OrderSagaState> transitions = new HashMap<>();

    @PostConstruct
    public void init() {
        register(OrderSagaState.ORDER_CREATED,
                SagaEventType.ORDER_CREATED,
                OrderSagaState.STOCK_RESERVATION_PENDING);

        register(OrderSagaState.STOCK_RESERVATION_PENDING,
                SagaEventType.STOCK_RESERVED,
                OrderSagaState.STOCK_RESERVED);

        register(OrderSagaState.STOCK_RESERVATION_PENDING,
                SagaEventType.STOCK_RESERVATION_FAILED,
                OrderSagaState.FAILED);

        register(OrderSagaState.STOCK_RESERVED,
                SagaEventType.PAYMENT_PROCESSED,
                OrderSagaState.COMPLETED);

        register(OrderSagaState.STOCK_RESERVED,
                SagaEventType.PAYMENT_FAILED,
                OrderSagaState.COMPENSATING);

        register(OrderSagaState.COMPENSATING,
                SagaEventType.SAGA_COMPENSATED,
                OrderSagaState.COMPENSATED);
    }

    private void register(
            OrderSagaState current,
            SagaEventType event,
            OrderSagaState next) {

        transitions.put(new SagaTransitionKey(current, event), next);
    }

    public OrderSagaState getNextState(
            OrderSagaState current,
            SagaEventType event) {

        return transitions.get(new SagaTransitionKey(current, event));
    }
}
