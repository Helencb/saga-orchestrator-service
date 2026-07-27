package helen.com.sagaorchestratorservice.compensation.handler;

import helen.com.sagaorchestratorservice.messaging.command.RefundPaymentCommand;
import helen.com.sagaorchestratorservice.messaging.producer.PaymentCommandProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompensationHandler implements CompensationHandler {
    private final PaymentCommandProducer producer;

    @Override
    public void compensate(UUID sagaId, UUID orderId, String correlationId) {
        log.info("Executing PAYMENT compensation for saga {}", sagaId);

        RefundPaymentCommand command = RefundPaymentCommand.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .correlationId(correlationId)
                .build();
        producer.refundPayment(command);
    }

    @Override
    public Integer getExecutionOrder() {
        return 2;
    }

    @Override
    public String getHandlerName() {
        return "PAYMENT_COMPENSATION";
    }

    @Override
    public String getRequiredStepName() {
        return "PROCESS_PAYMENT";
    }
}