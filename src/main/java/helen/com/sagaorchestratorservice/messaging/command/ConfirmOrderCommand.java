package helen.com.sagaorchestratorservice.messaging.command;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderCommand {
    private UUID sagaId;
    private UUID orderId;
    private String correlationId;
}
