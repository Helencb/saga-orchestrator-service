package helen.com.sagaorchestratorservice.messaging.event;

import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservedEvent {
    private UUID sagaId;
    private UUID orderId;
    private String correlationId;
}
