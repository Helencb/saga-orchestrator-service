package helen.com.sagaorchestratorservice.saga.persistence;

import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "saga_instance")
public class SagaInstanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID sagaId;

    @Column(nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSagaState currentState;

    @Column(nullable = false)
    private String correlationId;

    // guardados no início da saga para montar o ProcessPaymentCommand quando o estoque for reservado,
    // já que o evento StockReservedEvent não carrega esses dados de volta
    private UUID customerId;

    private BigDecimal totalAmount;

    // para rastrear quantos steps da compensação já foram confirmados
    @Column(nullable = false)
    @Builder.Default
    private Integer compensationStepsDone = 0;

    // flags de idempotência por step - evitam contar duas vezes a mesma confirmação
    // de compensação quando a fila (SQS, at-least-once) reentrega a mensagem
    @Column(nullable = false)
    @Builder.Default
    private boolean stockCompensated = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean paymentCompensated = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean orderCompensated = false;

    // para locking otimista — evita condição de corrida quando dois eventos chegam ao mesmo tempo para a mesma saga.
    @Version
    private Long version;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
