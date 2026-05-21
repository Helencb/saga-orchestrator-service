package helen.com.sagaorchestratorservice.messaging.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemPayload {
    private UUID productId;
    private Integer quantity;
    private BigDecimal price;
}
