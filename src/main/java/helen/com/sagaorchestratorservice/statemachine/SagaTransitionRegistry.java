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
        // Fluxo feliz
        register(OrderSagaState.ORDER_CREATED,
                SagaEventType.ORDER_CREATED,
                OrderSagaState.STOCK_RESERVATION_PENDING);

        register(OrderSagaState.STOCK_RESERVATION_PENDING,
                SagaEventType.STOCK_RESERVED,
                OrderSagaState.STOCK_RESERVED);

        register(OrderSagaState.STOCK_RESERVED,
                SagaEventType.PAYMENT_PROCESSED,
                OrderSagaState.ORDER_CONFIRM_PENDING);

        register(OrderSagaState.ORDER_CONFIRM_PENDING,
                SagaEventType.ORDER_CONFIRMED,
                OrderSagaState.COMPLETED);

        // Falhas
        register(OrderSagaState.STOCK_RESERVATION_PENDING,
                SagaEventType.STOCK_RESERVATION_FAILED,
                OrderSagaState.FAILED);

        register(OrderSagaState.STOCK_RESERVED,
                SagaEventType.PAYMENT_FAILED,
                OrderSagaState.COMPENSATING);

        register(OrderSagaState.ORDER_CONFIRM_PENDING,
                SagaEventType.PAYMENT_FAILED,
                OrderSagaState.COMPENSATING);

        // Compensações — cada retorno mantém o estado COMPENSATING até markCompensationStepDone() concluir os 3
        register(OrderSagaState.COMPENSATING,
                SagaEventType.STOCK_RELEASED,
                OrderSagaState.COMPENSATING);

        register(OrderSagaState.COMPENSATING,
                SagaEventType.PAYMENT_REFUNDED,
                OrderSagaState.COMPENSATING);

        register(OrderSagaState.COMPENSATING,
                SagaEventType.ORDER_CANCELLED,
                OrderSagaState.COMPENSATING);

        register(OrderSagaState.COMPENSATING,
                SagaEventType.SAGA_COMPENSATED,
                OrderSagaState.COMPENSATED);

        // Timeout - de qualquer estado ainda em andamento para FAILED.
        // Estados terminais (COMPLETED/FAILED/COMPENSATED) ficam de fora de propósito:
        // não existe transição registrada para eles, então o scheduler não consegue
        // sobrescrever uma saga que já terminou por um caminho legítimo.
        for (OrderSagaState state : OrderSagaState.values()) {
            if (state == OrderSagaState.COMPLETED
                    || state == OrderSagaState.FAILED
                    || state == OrderSagaState.COMPENSATED) {
                continue;
            }
            register(state, SagaEventType.SAGA_TIMEOUT, OrderSagaState.FAILED);
        }
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
