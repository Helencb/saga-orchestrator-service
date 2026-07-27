package helen.com.sagaorchestratorservice.messaging.event;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReleasedEvent {
    private UUID sagaId;
    private UUID orderId;
    private String correlationId;
}
