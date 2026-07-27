package helen.com.sagaorchestratorservice.saga.repository;

import helen.com.sagaorchestratorservice.saga.persistence.SagaInstanceEntity;
import helen.com.sagaorchestratorservice.saga.state.SagaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaInstanceRepository extends JpaRepository<SagaInstanceEntity, UUID> {
    Optional<SagaInstanceEntity> findBySagaId(UUID sagaId);

    Optional<SagaInstanceEntity> findByOrderId(UUID orderId);

    // Idempotência
    boolean existsBySagaId(UUID sagaId);

    // Listagem administrativa paginada
    Page<SagaInstanceEntity> findByStatus(SagaStatus status, Pageable pageable);

    @Query("""
        SELECT s FROM SagaInstanceEntity s
        WHERE s.status IN (:runningStatuses)
        AND s.updatedAt < :cutoff
    """)
    // Scheduler de timeout
    List<SagaInstanceEntity> findTimedOutSagas(
            @Param("cutoff")LocalDateTime cutoff,
            @Param("runningStatuses") List<SagaStatus> runningStatuses
            );

    default List<SagaInstanceEntity> findTimedOutSagas(LocalDateTime cutoff) {
        return findTimedOutSagas(cutoff, List.of(SagaStatus.STARTED, SagaStatus.RUNNING, SagaStatus.COMPENSATING));
    }

}
