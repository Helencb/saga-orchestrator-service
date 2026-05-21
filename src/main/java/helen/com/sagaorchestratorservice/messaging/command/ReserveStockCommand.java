package helen.com.sagaorchestratorservice.messaging.command;

import helen.com.sagaorchestratorservice.messaging.dto.OrderPayload;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockCommand {
    private UUID sagaId;
    private String correlationId;
    private OrderPayload order;
}
