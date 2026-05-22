package helen.com.sagaorchestratorservice.statemachine;

public class SagaStateMachineException extends RuntimeException{
    public SagaStateMachineException(String message) {
        super(message);
    }
}
