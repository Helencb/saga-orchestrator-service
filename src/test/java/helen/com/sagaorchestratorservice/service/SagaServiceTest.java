package helen.com.sagaorchestratorservice.service;

import helen.com.sagaorchestratorservice.saga.persistence.SagaInstanceEntity;
import helen.com.sagaorchestratorservice.saga.repository.SagaInstanceRepository;
import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaEventType;
import helen.com.sagaorchestratorservice.saga.state.SagaStatus;
import helen.com.sagaorchestratorservice.statemachine.OrderStateMachine;
import helen.com.sagaorchestratorservice.statemachine.SagaStateMachineException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SagaServiceTest {

    @Mock
    private SagaInstanceRepository repository;

    @Mock
    private OrderStateMachine stateMachine;

    private SagaService sagaService;

    private static final UUID SAGA_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sagaService = new SagaService(repository, stateMachine);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private SagaInstanceEntity sagaWith(SagaStatus status, OrderSagaState state, int compensationStepsDone) {
        return SagaInstanceEntity.builder()
                .sagaId(SAGA_ID)
                .orderId(UUID.randomUUID())
                .status(status)
                .currentState(state)
                .correlationId("corr-1")
                .compensationStepsDone(compensationStepsDone)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createSagaPersistsCustomerAndAmountForLaterPaymentStep() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("199.90");

        SagaInstanceEntity saved = sagaService.createSaga(SAGA_ID, orderId, "corr-1", customerId, amount);

        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getTotalAmount()).isEqualTo(amount);
        assertThat(saved.getStatus()).isEqualTo(SagaStatus.STARTED);
    }

    @Test
    void markCompensationStepDoneIgnoresDuplicateConfirmation() {
        SagaInstanceEntity saga = sagaWith(SagaStatus.COMPENSATING, OrderSagaState.COMPENSATING, 1);
        saga.setPaymentCompensated(true);
        when(repository.findBySagaId(SAGA_ID)).thenReturn(Optional.of(saga));

        sagaService.markCompensationStepDone(SAGA_ID, "PAYMENT");

        assertThat(saga.getCompensationStepsDone()).isEqualTo(1);
        verify(repository, never()).save(any());
    }

    @Test
    void markCompensationStepDoneIgnoresWhenSagaIsNotCompensating() {
        SagaInstanceEntity saga = sagaWith(SagaStatus.RUNNING, OrderSagaState.STOCK_RESERVED, 0);
        when(repository.findBySagaId(SAGA_ID)).thenReturn(Optional.of(saga));

        sagaService.markCompensationStepDone(SAGA_ID, "STOCK");

        assertThat(saga.isStockCompensated()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void markCompensationStepDoneClosesSagaOnceAllThreeStepsConfirmed() {
        SagaInstanceEntity saga = sagaWith(SagaStatus.COMPENSATING, OrderSagaState.COMPENSATING, 2);
        saga.setStockCompensated(true);
        saga.setPaymentCompensated(true);
        when(repository.findBySagaId(SAGA_ID)).thenReturn(Optional.of(saga));
        when(stateMachine.transition(OrderSagaState.COMPENSATING, SagaEventType.SAGA_COMPENSATED))
                .thenReturn(OrderSagaState.COMPENSATED);

        sagaService.markCompensationStepDone(SAGA_ID, "ORDER");

        assertThat(saga.isOrderCompensated()).isTrue();
        assertThat(saga.getCompensationStepsDone()).isEqualTo(3);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(saga.getCurrentState()).isEqualTo(OrderSagaState.COMPENSATED);
        verify(repository).save(saga);
    }

    @Test
    void markAsTimedOutSkipsSagaThatAlreadyReachedATerminalStateConcurrently() {
        SagaInstanceEntity saga = sagaWith(SagaStatus.COMPLETED, OrderSagaState.COMPLETED, 0);
        when(repository.findBySagaId(SAGA_ID)).thenReturn(Optional.of(saga));
        when(stateMachine.transition(eq(OrderSagaState.COMPLETED), eq(SagaEventType.SAGA_TIMEOUT)))
                .thenThrow(new SagaStateMachineException("Invalid transition"));

        sagaService.markAsTimedOut(SAGA_ID);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        verify(repository, never()).save(any());
    }

    @Test
    void markAsTimedOutMarksSagaAsTimeoutWhenTransitionIsValid() {
        SagaInstanceEntity saga = sagaWith(SagaStatus.RUNNING, OrderSagaState.STOCK_RESERVED, 0);
        when(repository.findBySagaId(SAGA_ID)).thenReturn(Optional.of(saga));
        when(stateMachine.transition(OrderSagaState.STOCK_RESERVED, SagaEventType.SAGA_TIMEOUT))
                .thenReturn(OrderSagaState.FAILED);

        sagaService.markAsTimedOut(SAGA_ID);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.TIMEOUT);
        assertThat(saga.getCurrentState()).isEqualTo(OrderSagaState.FAILED);
        verify(repository).save(saga);
    }
}
