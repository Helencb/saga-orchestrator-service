package helen.com.sagaorchestratorservice.exception;

import helen.com.sagaorchestratorservice.statemachine.SagaStateMachineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SagaNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(SagaNotFoundException ex) {
        log.warn("Saga not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(404, ex.getMessage()));
    }

    @ExceptionHandler(CompensationHandlerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCompensationHandlerNotFound(
            CompensationHandlerNotFoundException ex) {
        log.warn("Compensation handler not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(404, ex.getMessage()));
    }

    @ExceptionHandler(SagaStateMachineException.class)
    public ResponseEntity<Map<String, Object>> handleStateMachine(SagaStateMachineException ex) {
        log.error("Invalid saga transition: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(409, ex.getMessage()));
    }

    // Antes não existia: dois eventos chegando quase ao mesmo tempo para a
    // mesma saga geravam uma exceção 500 "crua" pro cliente/listener.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Concurrent update conflict on saga: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(409, "Concurrent update detected, please retry"));
    }

    @ExceptionHandler(MessagePublishException.class)
    public ResponseEntity<Map<String, Object>> handleMessagePublish(MessagePublishException ex) {
        log.error("Downstream messaging failure", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorBody(503, "Temporarily unable to dispatch command, please retry"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(500, "Internal server error"));
    }

    private Map<String, Object> errorBody(int status, String message) {
        return Map.of(
                "error", message,
                "timestamp", LocalDateTime.now(),
                "status", status
        );
    }
}