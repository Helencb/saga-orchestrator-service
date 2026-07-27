package helen.com.sagaorchestratorservice.service;

import helen.com.sagaorchestratorservice.exception.SagaNotFoundException;
import helen.com.sagaorchestratorservice.saga.persistence.SagaInstanceEntity;
import helen.com.sagaorchestratorservice.saga.repository.SagaInstanceRepository;
import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaEventType;
import helen.com.sagaorchestratorservice.saga.state.SagaStatus;
import helen.com.sagaorchestratorservice.statemachine.OrderStateMachine;
import helen.com.sagaorchestratorservice.statemachine.SagaStateMachineException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaService {
    private final SagaInstanceRepository repository;
    private final OrderStateMachine stateMachine;

    private static final List<String> COMPENSATION_STEPS = List.of("STOCK", "PAYMENT", "ORDER");

    @Transactional
    public SagaInstanceEntity createSaga(UUID sagaId, UUID orderId, String correlationId,
                                          UUID customerId, BigDecimal totalAmount) {
        SagaInstanceEntity entity = SagaInstanceEntity.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .status(SagaStatus.STARTED)
                .currentState(OrderSagaState.ORDER_CREATED)
                .correlationId(correlationId)
                .customerId(customerId)
                .totalAmount(totalAmount)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return repository.save(entity);
    }

    public boolean existsBySagaId(UUID sagaId) {
        return repository.existsBySagaId(sagaId);
    }

    public SagaInstanceEntity findBySagaId(UUID sagaId) {
        return repository.findBySagaId(sagaId)
                .orElseThrow(() -> new SagaNotFoundException("Saga not found"));
    }

    public SagaInstanceEntity findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId)
                .orElseThrow(() -> new SagaNotFoundException("Saga not found for order"));
    }

    public void updateState(UUID sagaId, SagaEventType eventType, SagaStatus status) {
        SagaInstanceEntity saga = findBySagaId(sagaId);

        OrderSagaState nextState = stateMachine
                .transition(saga.getCurrentState(), eventType);

        log.info("Saga {} transitioning: {} --[{}]--> {}",
                                sagaId, saga.getCurrentState(), eventType, nextState);

        saga.setCurrentState(nextState);
        saga.setStatus(status);
        saga.setUpdatedAt(LocalDateTime.now());
        repository.save(saga);
    }

    @Transactional
    public void markCompensationStepDone(UUID sagaId, String stepName) {
        SagaInstanceEntity saga = findBySagaId(sagaId);

        if (saga.getStatus() != SagaStatus.COMPENSATING) {
            log.warn("Ignoring compensation step {} for saga {}: saga is not COMPENSATING (status={})",
                    stepName, sagaId, saga.getStatus());
            return;
        }

        if (isStepAlreadyCompensated(saga, stepName)) {
            log.warn("Ignoring duplicate compensation confirmation for step {} on saga {} " +
                    "(likely a redelivered message)", stepName, sagaId);
            return;
        }
        markStepCompensated(saga, stepName);

        int done = saga.getCompensationStepsDone() + 1;
        saga.setCompensationStepsDone(done);
        saga.setUpdatedAt(LocalDateTime.now());

        log.info("Saga {} compensation step {} done ({}/{})",
                sagaId, stepName, done, COMPENSATION_STEPS.size());

        if (done >= COMPENSATION_STEPS.size()) {
            OrderSagaState nextState = stateMachine
                    .transition(saga.getCurrentState(), SagaEventType.SAGA_COMPENSATED);

            saga.setCurrentState(nextState);
            saga.setStatus(SagaStatus.COMPENSATED);
            log.info("Saga {} fully compensated", sagaId);
        }

        repository.save(saga);
    }

    private boolean isStepAlreadyCompensated(SagaInstanceEntity saga, String stepName) {
        return switch (stepName) {
            case "STOCK" -> saga.isStockCompensated();
            case "PAYMENT" -> saga.isPaymentCompensated();
            case "ORDER" -> saga.isOrderCompensated();
            default -> throw new IllegalArgumentException("Unknown compensation step: " + stepName);
        };
    }

    private void markStepCompensated(SagaInstanceEntity saga, String stepName) {
        switch (stepName) {
            case "STOCK" -> saga.setStockCompensated(true);
            case "PAYMENT" -> saga.setPaymentCompensated(true);
            case "ORDER" -> saga.setOrderCompensated(true);
            default -> throw new IllegalArgumentException("Unknown compensation step: " + stepName);
        }
    }

    @Transactional
    public void markAsTimedOut(UUID sagaId) {
        SagaInstanceEntity saga = findBySagaId(sagaId);

        OrderSagaState nextState;
        try {
            nextState = stateMachine.transition(saga.getCurrentState(), SagaEventType.SAGA_TIMEOUT);
        } catch (SagaStateMachineException ex) {
            // A saga já saiu do estado que o scheduler enxergou (terminou com sucesso ou já
            // falhou por outro caminho entre a consulta de timeout e esta chamada) - não sobrescrever.
            log.warn("Saga {} no longer eligible for timeout (current state: {}): {}",
                    sagaId, saga.getCurrentState(), ex.getMessage());
            return;
        }

        saga.setStatus(SagaStatus.TIMEOUT);
        saga.setCurrentState(nextState);
        saga.setUpdatedAt(LocalDateTime.now());
        repository.save(saga);
        log.warn("Saga {} marked as TIMEOUT", sagaId);
    }

    public List<SagaInstanceEntity> findTimedOutSagas(int timeoutSeconds) {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);
        return repository.findTimedOutSagas(cutoff);
    }
}
