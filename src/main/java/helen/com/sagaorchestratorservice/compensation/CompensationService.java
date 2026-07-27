package helen.com.sagaorchestratorservice.compensation;

import helen.com.sagaorchestratorservice.config.SagaProperties;
import helen.com.sagaorchestratorservice.messaging.command.RetryCompensationCommand;
import helen.com.sagaorchestratorservice.saga.state.SagaStepStatus;
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
    private final SagaStepService stepService;
    private final SagaProperties sagaProperties;

    /**
     * Dispara os comandos de compensação via SNS (assíncrono).
     * O estado COMPENSATED só é marcado quando todos os eventos de retorno
     * (StockReleased, PaymentRefunded, OrderCancelled) forem recebidos
     * via SagaService.markCompensationStepDone().
     */
    public void startCompensation(UUID sagaId, UUID orderId, String correlationId) {
        log.warn("Starting compensation for saga {}", sagaId);

        stepService.saveStep(sagaId, "COMPENSATION_STARTED", SagaStepStatus.RUNNING, null);
        executor.execute(sagaId, orderId, correlationId);
        log.info("Compensation commands dispatched for saga {} - awaiting confirmation events", sagaId);
    }

    /**
     * Chamado pelo RetryCompensationConsumer quando um handler de compensação
     * falhou ao publicar seu comando na primeira tentativa.
     */
    public void retryCompensation(RetryCompensationCommand command) {
        int attempt = command.getRetryCount() + 1;

        if (command.getRetryCount() >= sagaProperties.getMaxRetry()) {
            log.error("Compensation handler {} for saga {} exhausted {} retries - " +
                            "requires MANUAL intervention",
                    command.getHandlerName(), command.getSagaId(), sagaProperties.getMaxRetry());

            stepService.saveStep(command.getSagaId(),
                    command.getHandlerName() + "_ABANDONED",
                    SagaStepStatus.FAILED,
                    "Exceeded max-retry (" + sagaProperties.getMaxRetry() + "). Manual intervention required.");
            return;
        }

        executor.retry(command.getHandlerName(), command.getSagaId(),
                command.getOrderId(), command.getCorrelationId(), attempt);
    }
}