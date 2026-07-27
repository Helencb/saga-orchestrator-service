package helen.com.sagaorchestratorservice.scheduler;

import helen.com.sagaorchestratorservice.compensation.CompensationService;
import helen.com.sagaorchestratorservice.config.SagaProperties;
import helen.com.sagaorchestratorservice.saga.persistence.SagaInstanceEntity;
import helen.com.sagaorchestratorservice.saga.state.SagaStepStatus;
import helen.com.sagaorchestratorservice.service.SagaService;
import helen.com.sagaorchestratorservice.service.SagaStepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaTimeoutScheduler {

    private final SagaService service;
    private final SagaStepService stepService;
    private final CompensationService compensationService;
    private final SagaProperties sagaProperties;

    @Scheduled(fixedDelayString = "${saga.timeout-check-interval-ms:60000}")
    public void checkTimeouts() {
        List<SagaInstanceEntity> timedOut =
                service.findTimedOutSagas(sagaProperties.getTimeoutSeconds());
        if (timedOut.isEmpty()) return;

        log.warn("Found {} timed_out sagas", timedOut.size());

        for (SagaInstanceEntity saga : timedOut) {
            log.warn("Marking saga {} as TIMEOUT (state: {}, orderId: {})",
                    saga.getSagaId(), saga.getCurrentState(), saga.getOrderId());
            service.markAsTimedOut(saga.getSagaId());

            stepService.saveStep(
                    saga.getSagaId(),
                    "TIMEOUT",
                    SagaStepStatus.FAILED,
                    "Saga exceeded timeout of " + sagaProperties.getTimeoutSeconds() + "s");

            // Antes o saga ficava travado em TIMEOUT para sempre - qualquer estoque
            // reservado ou pagamento processado nunca era desfeito. Agora o timeout
            // dispara a compensação dos steps que já tiverem sido concluídos.
            compensationService.startCompensation(
                    saga.getSagaId(), saga.getOrderId(), saga.getCorrelationId());
        }
    }
}