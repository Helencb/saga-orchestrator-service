package helen.com.sagaorchestratorservice.compensation;

import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaStatus;
import helen.com.sagaorchestratorservice.saga.state.SagaStepStatus;
import helen.com.sagaorchestratorservice.service.SagaService;
import helen.com.sagaorchestratorservice.service.SagaStepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationService {
    private final CompensationExecutor executor;
    private final SagaService sagaService;
    private final SagaStepService stepService;

    public void startCompensation(UUID sagaId, UUID orderId, String correlationId) {
        log.error("Starting compensation flow for saga {}", sagaId);

        sagaService.updateState(
                sagaId,
                OrderSagaState.COMPENSATING,
                SagaStatus.COMPENSATING);

        executor.execute(
                sagaId,
                orderId,
                correlationId);

        sagaService.updateState(
                sagaId,
                OrderSagaState.COMPENSATED,
                SagaStatus.COMPENSATED);

        stepService.saveStep(
                sagaId,
                "COMPENSATION_FINISHED",
                SagaStepStatus.SUCCESS,
                null);

        log.info("Compensation completed for saga {}", sagaId);
    }
}
