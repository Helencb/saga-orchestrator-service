package helen.com.sagaorchestratorservice.messaging.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPayload {
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
}
