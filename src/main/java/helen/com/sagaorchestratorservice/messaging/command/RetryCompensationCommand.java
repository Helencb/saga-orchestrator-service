package helen.com.sagaorchestratorservice.messaging.command;

import lombok.*;

import java.util.UUID;

/**
 * Publicado quando um CompensationHandler falha ao enviar seu comando
 * (ex.: SNS fora do ar). Antes essa classe existia mas nunca era usada -
 * nenhuma compensação com falha era reprocessada.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryCompensationCommand {
    private UUID sagaId;
    private UUID orderId;
    private String handlerName;
    private Integer retryCount;
    private String correlationId;
}