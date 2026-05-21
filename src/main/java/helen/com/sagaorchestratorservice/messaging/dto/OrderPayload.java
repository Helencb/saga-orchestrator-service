package helen.com.sagaorchestratorservice.messaging.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPayload {
    private UUID orderId;
    private UUID customerId;
    private List<OrderItemPayload> items;
    private BigDecimal totalAmount;
}
