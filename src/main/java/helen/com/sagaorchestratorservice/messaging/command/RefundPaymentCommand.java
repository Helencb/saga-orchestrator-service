package helen.com.sagaorchestratorservice.messaging.command;

import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPaymentCommand {
    private UUID sagaId;
    private UUID orderId;
    private String correlationId;
}
