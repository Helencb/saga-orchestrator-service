package helen.com.sagaorchestratorservice.statemachine;

import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaEventType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SagaTransition {
    private OrderSagaState currentState;
    private SagaEventType eventType;
    private OrderSagaState nextState;
}
