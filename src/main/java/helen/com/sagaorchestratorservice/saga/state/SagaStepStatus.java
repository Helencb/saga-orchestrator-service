package helen.com.sagaorchestratorservice.saga.state;

public enum SagaStepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    COMPENSATED,
    SKIPPED
}
