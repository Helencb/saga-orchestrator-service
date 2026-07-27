package helen.com.sagaorchestratorservice.messaging.producer;

import helen.com.sagaorchestratorservice.exception.MessagePublishException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaCommandProducer {
    private final RabbitTemplate rabbitTemplate;

    public void publish(String exchange, String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (AmqpException ex) {
            log.error("Failed to publish to exchange {} with routing key {}", exchange, routingKey, ex);
            // preserva a causa original (antes o stack trace era perdido aqui)
            throw new MessagePublishException(exchange, routingKey, ex);
        }
    }
}
