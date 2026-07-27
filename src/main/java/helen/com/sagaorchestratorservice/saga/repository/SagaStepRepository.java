package helen.com.sagaorchestratorservice.saga.repository;

import helen.com.sagaorchestratorservice.saga.persistence.SagaStepEntity;
import helen.com.sagaorchestratorservice.saga.state.SagaStepStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SagaStepRepository extends JpaRepository<SagaStepEntity, UUID> {
    List<SagaStepEntity> findBySagaIdOrderByCreatedAtAsc(UUID sagaId);

    // usado pelo CompensationExecutor para saber quais steps de fato
    // aconteceram com sucesso e, portanto, precisam ser compensados
    List<SagaStepEntity> findBySagaIdAndStatus(UUID sagaId, SagaStepStatus status);
}