package helen.com.sagaorchestratorservice.compensation;

import helen.com.sagaorchestratorservice.config.SagaProperties;
import helen.com.sagaorchestratorservice.exception.CompensationHandlerNotFoundException;
import helen.com.sagaorchestratorservice.messaging.command.RetryCompensationCommand;
import helen.com.sagaorchestratorservice.saga.persistence.SagaInstanceEntity;
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
    private final SagaStepService stepService;
    private final SagaProperties sagaProperties;
    private final SagaService sagaService;

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

    /**
     * Endpoint administrativo: força uma saga travada a entrar em compensação
     * sem esperar o scheduler de timeout.
     */
    public void forceCompensation(UUID sagaId) {
        SagaInstanceEntity saga = sagaService.forceCompensation(sagaId);
        stepService.saveStep(sagaId, "MANUAL_COMPENSATION_TRIGGERED", SagaStepStatus.RUNNING, null);
        startCompensation(sagaId, saga.getOrderId(), saga.getCorrelationId());
    }

    /**
     * Endpoint administrativo: reexecuta manualmente um handler que foi
     * abandonado após esgotar o limite de retries automáticos.
     */
    public void forceRetryHandler(String handlerName, UUID sagaId, UUID orderId, String correlationId) {
        if (!executor.handlerExists(handlerName)) {
            throw new CompensationHandlerNotFoundException(
                    "No compensation handler found named " + handlerName);
        }

        log.warn("Manually retrying compensation handler {} for saga {}", handlerName, sagaId);
        stepService.saveStep(sagaId, handlerName + "_MANUAL_RETRY", SagaStepStatus.RUNNING, null);
        executor.retry(handlerName, sagaId, orderId, correlationId, 1);
    }
}