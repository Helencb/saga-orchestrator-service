package helen.com.sagaorchestratorservice.compensation;

import helen.com.sagaorchestratorservice.compensation.handler.CompensationHandler;
import helen.com.sagaorchestratorservice.messaging.producer.CompensationRetryProducer;
import helen.com.sagaorchestratorservice.saga.persistence.SagaStepEntity;
import helen.com.sagaorchestratorservice.saga.repository.SagaStepRepository;
import helen.com.sagaorchestratorservice.saga.state.SagaStepStatus;
import helen.com.sagaorchestratorservice.service.SagaStepService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompensationExecutorTest {

    @Mock
    private SagaStepRepository stepRepository;

    @Mock
    private SagaStepService stepService;

    @Mock
    private CompensationRetryProducer retryProducer;

    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    private List<String> executionLog;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executionLog = new ArrayList<>();
    }

    private CompensationHandler handler(String name, String requiredStep, int order, boolean fails) {
        return new CompensationHandler() {
            @Override
            public void compensate(UUID sagaId, UUID orderId, String correlationId) {
                executionLog.add(name);
                if (fails) {
                    throw new RuntimeException(name + " failed");
                }
            }

            @Override
            public Integer getExecutionOrder() {
                return order;
            }

            @Override
            public String getHandlerName() {
                return name;
            }

            @Override
            public String getRequiredStepName() {
                return requiredStep;
            }
        };
    }

    private SagaStepEntity succeededStep(String name) {
        return SagaStepEntity.builder().sagaId(SAGA_ID).stepName(name).status(SagaStepStatus.SUCCESS).build();
    }

    @Test
    void executesOnlyHandlersWhoseRequiredStepSucceeded_inReverseExecutionOrder() {
        CompensationHandler stock = handler("STOCK_COMPENSATION", "RESERVE_STOCK", 1, false);
        CompensationHandler payment = handler("PAYMENT_COMPENSATION", "PROCESS_PAYMENT", 2, false);
        CompensationHandler order = handler("ORDER_COMPENSATION", "CONFIRM_ORDER", 3, false);

        // CONFIRM_ORDER nunca teve sucesso (pagamento falhou antes disso) -
        // ORDER_COMPENSATION não deve rodar.
        when(stepRepository.findBySagaIdAndStatus(SAGA_ID, SagaStepStatus.SUCCESS))
                .thenReturn(List.of(succeededStep("RESERVE_STOCK"), succeededStep("PROCESS_PAYMENT")));

        CompensationExecutor executor = new CompensationExecutor(
                List.of(stock, payment, order), stepRepository, stepService, retryProducer);

        executor.execute(SAGA_ID, ORDER_ID, "corr-1");

        assertThat(executionLog).containsExactly("PAYMENT_COMPENSATION", "STOCK_COMPENSATION");
    }

    @Test
    void aFailingHandlerSchedulesRetryButDoesNotBlockTheOthers() {
        CompensationHandler stock = handler("STOCK_COMPENSATION", "RESERVE_STOCK", 1, true);
        CompensationHandler payment = handler("PAYMENT_COMPENSATION", "PROCESS_PAYMENT", 2, false);

        when(stepRepository.findBySagaIdAndStatus(SAGA_ID, SagaStepStatus.SUCCESS))
                .thenReturn(List.of(succeededStep("RESERVE_STOCK"), succeededStep("PROCESS_PAYMENT")));

        CompensationExecutor executor = new CompensationExecutor(
                List.of(stock, payment), stepRepository, stepService, retryProducer);

        executor.execute(SAGA_ID, ORDER_ID, "corr-1");

        assertThat(executionLog).containsExactly("PAYMENT_COMPENSATION", "STOCK_COMPENSATION");
        verify(retryProducer).scheduleRetry(argThat(cmd ->
                cmd.getHandlerName().equals("STOCK_COMPENSATION") && cmd.getRetryCount() == 1));
        verify(stepService).saveStep(eq(SAGA_ID), eq("STOCK_COMPENSATION_RETRY_SCHEDULED"),
                eq(SagaStepStatus.FAILED), any());
    }

    @Test
    void handlerExistsReflectsRegisteredHandlers() {
        CompensationHandler stock = handler("STOCK_COMPENSATION", "RESERVE_STOCK", 1, false);
        CompensationExecutor executor = new CompensationExecutor(
                List.of(stock), stepRepository, stepService, retryProducer);

        assertThat(executor.handlerExists("STOCK_COMPENSATION")).isTrue();
        assertThat(executor.handlerExists("UNKNOWN")).isFalse();
    }
}
