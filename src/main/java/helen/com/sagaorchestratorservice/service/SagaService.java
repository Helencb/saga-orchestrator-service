package helen.com.sagaorchestratorservice.service;

import helen.com.sagaorchestratorservice.exception.SagaNotFoundException;
import helen.com.sagaorchestratorservice.saga.persistence.SagaInstanceEntity;
import helen.com.sagaorchestratorservice.saga.repository.SagaInstanceRepository;
import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaEventType;
import helen.com.sagaorchestratorservice.saga.state.SagaStatus;
import helen.com.sagaorchestratorservice.statemachine.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SagaService {
    private final SagaInstanceRepository repository;
    private final OrderStateMachine stateMachine;

    public SagaInstanceEntity createSaga(UUID sagaId, UUID orderId, String correlationId) {
        SagaInstanceEntity entity = SagaInstanceEntity.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .status(SagaStatus.STARTED)
                .currentState(OrderSagaState.ORDER_CREATED)
                .correlationId(correlationId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return repository.save(entity);
    }

    public SagaInstanceEntity findBySagaId(UUID sagaId) {
        return repository.findBySagaId(sagaId)
                .orElseThrow(() -> new SagaNotFoundException("Saga not found"));
    }

    public void updateState(UUID sagaId, SagaEventType eventType, SagaStatus status) {
        SagaInstanceEntity saga = findBySagaId(sagaId);

        OrderSagaState nextState = stateMachine
                .transition(saga.getCurrentState(), eventType);

        saga.setCurrentState(nextState);
        saga.setStatus(status);
        saga.setUpdatedAt(LocalDateTime.now());
        repository.save(saga);
    }
}
