package helen.com.sagaorchestratorservice.compensation.handler;

import java.util.UUID;

public interface CompensationHandler {
    void compensate(UUID sagaId, UUID orderId, String correlationId);

    Integer getExecutionOrder();

    String getHandlerName();
}
