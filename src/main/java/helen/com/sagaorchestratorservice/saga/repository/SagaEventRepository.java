package helen.com.sagaorchestratorservice.saga.repository;

import helen.com.sagaorchestratorservice.saga.persistence.SagaEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SagaEventRepository extends JpaRepository<SagaEventEntity, UUID> {
    List<SagaEventEntity> findBySagaId(UUID sagaId);
}
