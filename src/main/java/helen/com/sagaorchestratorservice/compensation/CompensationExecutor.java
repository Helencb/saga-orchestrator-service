package helen.com.sagaorchestratorservice.compensation;

import helen.com.sagaorchestratorservice.compensation.handler.CompensationHandler;
import helen.com.sagaorchestratorservice.messaging.command.RetryCompensationCommand;
import helen.com.sagaorchestratorservice.messaging.producer.CompensationRetryProducer;
import helen.com.sagaorchestratorservice.saga.persistence.SagaStepEntity;
import helen.com.sagaorchestratorservice.saga.repository.SagaStepRepository;
import helen.com.sagaorchestratorservice.saga.state.SagaStepStatus;
import helen.com.sagaorchestratorservice.service.SagaStepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationExecutor {
    private final List<CompensationHandler> handlers;
    private final SagaStepRepository stepRepository;
    private final SagaStepService stepService;
    private final CompensationRetryProducer retryProducer;

    public void execute(UUID sagaId, UUID orderId, String correlationId) {
        Set<String> succeededSteps = stepRepository
                .findBySagaIdAndStatus(sagaId, SagaStepStatus.SUCCESS)
                .stream()
                .map(SagaStepEntity::getStepName)
                .collect(Collectors.toSet());

        handlers.stream()
                .filter(handler -> succeededSteps.contains(handler.getRequiredStepName()))
                .sorted(Comparator.comparing(CompensationHandler::getExecutionOrder).reversed())
                .forEach(handler -> runHandler(handler, sagaId, orderId, correlationId, 1));

        if (succeededSteps.isEmpty()) {
            log.info("Saga {} has no succeeded steps to compensate - nothing to do", sagaId);
        }
    }

    /**
     * Reexecuta um único handler (chamado pelo consumidor de retry).
     * Não filtra por step porque, se chegou aqui, já foi decidido antes que
     * esse handler era aplicável.
     */
    public void retry(String handlerName, UUID sagaId, UUID orderId, String correlationId, int attempt) {
        handlers.stream()
                .filter(h -> h.getHandlerName().equals(handlerName))
                .findFirst()
                .ifPresentOrElse(
                        handler -> runHandler(handler, sagaId, orderId, correlationId, attempt),
                        () -> log.error("No compensation handler found named {}", handlerName)
                );
    }

    private void runHandler(CompensationHandler handler, UUID sagaId, UUID orderId,
                            String correlationId, int attempt) {
        log.info("Executing compensation handler {} for saga {} (attempt {})",
                handler.getHandlerName(), sagaId, attempt);
        try {
            handler.compensate(sagaId, orderId, correlationId);
        } catch (Exception ex) {
            // Uma falha aqui não pode impedir os outros handlers de rodar -
            // por isso o catch fica dentro do loop, não em volta dele.
            log.error("Compensation handler {} failed for saga {} (attempt {})",
                    handler.getHandlerName(), sagaId, attempt, ex);

            RetryCompensationCommand retryCommand = RetryCompensationCommand.builder()
                    .sagaId(sagaId)
                    .orderId(orderId)
                    .handlerName(handler.getHandlerName())
                    .retryCount(attempt)
                    .correlationId(correlationId)
                    .build();

            stepService.saveStep(sagaId, handler.getHandlerName() + "_RETRY_SCHEDULED",
                    SagaStepStatus.FAILED, ex.getMessage());

            retryProducer.scheduleRetry(retryCommand);
        }
    }
}