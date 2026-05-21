package helen.com.sagaorchestratorservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import helen.com.sagaorchestratorservice.saga.persistence.SagaEventEntity;
import helen.com.sagaorchestratorservice.saga.repository.SagaEventRepository;
import helen.com.sagaorchestratorservice.saga.state.SagaEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SagaEventService {
    private final SagaEventRepository repository;
    private final ObjectMapper objectMapper;
    
    public void saveEvent(UUID sagaId, SagaEventType type,
                          Object payload, String correlationId) {
        try {
            SagaEventEntity entity = SagaEventEntity.builder()
                    .sagaId(sagaId)
                    .eventType(type)
                    .payload(objectMapper.writeValueAsString(payload))
                    .correlationId(correlationId)
                    .createdAt(LocalDateTime.now())
                    .build();

            repository.save(entity);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }
}
