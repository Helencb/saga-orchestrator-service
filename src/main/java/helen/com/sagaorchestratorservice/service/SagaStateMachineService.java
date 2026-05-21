package helen.com.sagaorchestratorservice.service;

import helen.com.sagaorchestratorservice.exception.InvalidSagaStateException;
import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import org.springframework.stereotype.Service;

@Service
public class SagaStateMachineService {
    public void validateTransition(OrderSagaState current, OrderSagaState next) {
        if (current == OrderSagaState.FAILED) {
            throw new InvalidSagaStateException("Saga already failed");
        }

        if (current == OrderSagaState.COMPLETED) {
            throw new InvalidSagaStateException("Saga already completed");
        }

        if (current == next) {
            throw new InvalidSagaStateException("Invalid duplicated state transition");
        }
    }
}
