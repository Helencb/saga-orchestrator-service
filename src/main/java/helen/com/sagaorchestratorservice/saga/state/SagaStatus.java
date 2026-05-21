package helen.com.sagaorchestratorservice.saga.state;

public enum SagaStatus {
    STARTED,
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    CANCELED,
    TIMEOUT
}
