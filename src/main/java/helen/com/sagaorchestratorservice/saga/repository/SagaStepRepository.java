package helen.com.sagaorchestratorservice.saga.repository;

import helen.com.sagaorchestratorservice.saga.persistence.SagaStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SagaStepRepository extends JpaRepository<SagaStepEntity, UUID> {
    List<SagaStepEntity> findBySagaId(UUID sagaId);
}
