package helen.com.sagaorchestratorservice.exception;

public class InvalidSagaStateException extends RuntimeException{
    public InvalidSagaStateException(String message){
        super(message);
    }
}
