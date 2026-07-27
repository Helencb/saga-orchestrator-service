package helen.com.sagaorchestratorservice.compensation.handler;

import java.util.UUID;

public interface CompensationHandler {
    void compensate(UUID sagaId, UUID orderId, String correlationId);

    Integer getExecutionOrder();

    String getHandlerName();

    /**
     * Nome do step (SagaStepEntity.stepName) que precisa ter terminado com
     * SUCCESS para que esta compensação faça sentido.
     * Ex.: STOCK só deve ser liberado se RESERVE_STOCK teve sucesso;
     * não faz sentido mandar ReleaseStockCommand se o estoque nunca foi reservado.
     */
    String getRequiredStepName();
}