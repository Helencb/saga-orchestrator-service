package helen.com.sagaorchestratorservice.messaging.event;

import helen.com.sagaorchestratorservice.messaging.dto.OrderPayload;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private UUID sagaId;
    private String correlationId;
    private OrderPayload order;
}
