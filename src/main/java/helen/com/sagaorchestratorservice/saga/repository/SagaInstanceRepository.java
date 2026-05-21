package helen.com.sagaorchestratorservice.saga.repository;

import helen.com.sagaorchestratorservice.saga.persistence.SagaInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SagaInstanceRepository extends JpaRepository<SagaInstanceEntity, UUID> {
    Optional<SagaInstanceEntity> findBySagaId(UUID sagaId);

    Optional<SagaInstanceEntity> findByOrderId(UUID orderId);

}
