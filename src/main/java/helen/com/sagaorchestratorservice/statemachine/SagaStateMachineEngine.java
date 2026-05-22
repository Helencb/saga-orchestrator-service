package helen.com.sagaorchestratorservice.statemachine;

import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SagaStateMachineEngine {
    private final SagaTransitionValidator validator;

    public OrderSagaState nextState(
            OrderSagaState currentState,
            SagaEventType eventType) {

        return validator.validate(currentState, eventType);
    }
}
