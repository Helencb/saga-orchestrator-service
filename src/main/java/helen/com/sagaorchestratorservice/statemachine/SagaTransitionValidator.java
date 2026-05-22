package helen.com.sagaorchestratorservice.statemachine;

import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SagaTransitionValidator {
    private final SagaTransitionRegistry registry;

    public OrderSagaState validate(OrderSagaState currentState, SagaEventType eventType) {
        OrderSagaState nextState = registry
                .getNextState(currentState, eventType);

        if (nextState == null) {
            throw new SagaStateMachineException(
                    "Invalid transition. Current state: "
                            + currentState
                            + ", event: "
                            + eventType);
        }

        return nextState;
    }
}
