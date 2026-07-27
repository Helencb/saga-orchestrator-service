package helen.com.sagaorchestratorservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ: um topic exchange por domínio (order/payment/inventory/retry),
 * com uma routing key por comando publicado e por evento consumido. As filas
 * declaradas aqui são só as que este serviço consome - quem publica os eventos
 * (order-service, payment-service, stock-service) fica fora deste repositório.
 */
@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {
    private final SagaProperties sagaProperties;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(sagaProperties.getOrderExchange());
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(sagaProperties.getPaymentExchange());
    }

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(sagaProperties.getInventoryExchange());
    }

    @Bean
    public TopicExchange retryExchange() {
        return new TopicExchange(sagaProperties.getRetryExchange());
    }

    private String queue(String key) {
        return sagaProperties.getQueues().get(key);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(queue("order-created"), true);
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return new Queue(queue("order-confirmed"), true);
    }

    @Bean
    public Queue orderCancelledQueue() {
        return new Queue(queue("order-cancelled"), true);
    }

    @Bean
    public Queue paymentProcessedQueue() {
        return new Queue(queue("payment-processed"), true);
    }

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(queue("payment-failed"), true);
    }

    @Bean
    public Queue paymentRefundedQueue() {
        return new Queue(queue("payment-refunded"), true);
    }

    @Bean
    public Queue stockReservedQueue() {
        return new Queue(queue("stock-reserved"), true);
    }

    @Bean
    public Queue stockFailedQueue() {
        return new Queue(queue("stock-failed"), true);
    }

    @Bean
    public Queue stockReleasedQueue() {
        return new Queue(queue("stock-released"), true);
    }

    @Bean
    public Queue retryCompensationQueue() {
        return new Queue(queue("retry-compensation"), true);
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(orderExchange()).with("order.created");
    }

    @Bean
    public Binding orderConfirmedBinding() {
        return BindingBuilder.bind(orderConfirmedQueue()).to(orderExchange()).with("order.confirmed");
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(orderCancelledQueue()).to(orderExchange()).with("order.cancelled");
    }

    @Bean
    public Binding paymentProcessedBinding() {
        return BindingBuilder.bind(paymentProcessedQueue()).to(paymentExchange()).with("payment.processed");
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue()).to(paymentExchange()).with("payment.failed");
    }

    @Bean
    public Binding paymentRefundedBinding() {
        return BindingBuilder.bind(paymentRefundedQueue()).to(paymentExchange()).with("payment.refunded");
    }

    @Bean
    public Binding stockReservedBinding() {
        return BindingBuilder.bind(stockReservedQueue()).to(inventoryExchange()).with("stock.reserved");
    }

    @Bean
    public Binding stockFailedBinding() {
        return BindingBuilder.bind(stockFailedQueue()).to(inventoryExchange()).with("stock.failed");
    }

    @Bean
    public Binding stockReleasedBinding() {
        return BindingBuilder.bind(stockReleasedQueue()).to(inventoryExchange()).with("stock.released");
    }

    @Bean
    public Binding retryCompensationBinding() {
        return BindingBuilder.bind(retryCompensationQueue()).to(retryExchange()).with("compensation.retry");
    }
}
