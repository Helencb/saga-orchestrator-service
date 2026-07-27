package helen.com.sagaorchestratorservice.messaging.producer;

import helen.com.sagaorchestratorservice.messaging.command.ProcessPaymentCommand;
import helen.com.sagaorchestratorservice.messaging.command.RefundPaymentCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCommandProducer {
    private final SagaCommandProducer producer;

    @Value("${saga.payment-exchange}")
    private String paymentExchange;

    public void processPayment(ProcessPaymentCommand command) {
        producer.publish(paymentExchange, "payment.process", command);
    }

    public void refundPayment(RefundPaymentCommand command) {
        producer.publish(paymentExchange, "payment.refund", command);
    }
}
