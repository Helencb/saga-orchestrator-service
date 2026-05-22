package helen.com.sagaorchestratorservice.compensation;

import helen.com.sagaorchestratorservice.compensation.handler.CompensationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationExecutor {
    private final List<CompensationHandler> handlers;

    public void execute(UUID sagaId, UUID orderId, String correlationId) {
        handlers.stream()
                .sorted(Comparator.comparing(CompensationHandler::getExecutionOrder)
                        .reversed()).forEach(handler -> {
                            log.info("Executing compensation handler {}",
                                    handler.getHandlerName());

                            handler.compensate(sagaId, orderId, correlationId);
                });
    }
}
