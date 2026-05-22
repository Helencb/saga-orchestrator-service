package helen.com.sagaorchestratorservice.messaging.command;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaCompensationCommand {
    private UUID sagaId;
    private String orderId;
    private String correlationId;
    private String reason;
}
