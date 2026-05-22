package helen.com.sagaorchestratorservice.statemachine;

import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStateMachine {
    private final SagaStateMachineEngine engine;

    public OrderSagaState transition(
            OrderSagaState currentState,
            SagaEventType eventType) {

        return engine.nextState(
                currentState,
                eventType);
    }
}
