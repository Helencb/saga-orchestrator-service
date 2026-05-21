package helen.com.sagaorchestratorservice.messaging.command;

import helen.com.sagaorchestratorservice.messaging.dto.PaymentPayload;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentCommand {
    private UUID sagaId;
    private String correlationId;
    private PaymentPayload payment;
}
