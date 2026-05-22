package helen.com.sagaorchestratorservice.messaging.command;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryCompensationCommand {
    private UUID sagaId;
    private Integer retryCount;
    private String correlationId;
}
