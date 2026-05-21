package helen.com.sagaorchestratorservice.service;

import helen.com.sagaorchestratorservice.saga.persistence.SagaStepEntity;
import helen.com.sagaorchestratorservice.saga.repository.SagaStepRepository;
import helen.com.sagaorchestratorservice.saga.state.SagaStepStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SagaStepService {
    private final SagaStepRepository repository;

    public void saveStep(UUID sagaId, String stepName,
                         SagaStepStatus status, String errorMessage) {
        SagaStepEntity entity = SagaStepEntity.builder()
                .sagaId(sagaId)
                .stepName(stepName)
                .status(status)
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .updateAt(LocalDateTime.now())
                .build();
        repository.save(entity);
    }
}
