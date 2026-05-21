package helen.com.sagaorchestratorservice.exception;

public class SagaNotFoundException extends RuntimeException{
    public SagaNotFoundException(String message){
        super(message);
    }
}
