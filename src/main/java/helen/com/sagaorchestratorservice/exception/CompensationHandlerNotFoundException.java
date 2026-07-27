package helen.com.sagaorchestratorservice.exception;

public class CompensationHandlerNotFoundException extends RuntimeException {
    public CompensationHandlerNotFoundException(String message) {
        super(message);
    }
}
