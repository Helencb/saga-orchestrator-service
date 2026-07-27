package helen.com.sagaorchestratorservice.exception;

/**
 * Lançada quando a publicação de uma mensagem no RabbitMQ falha.
 * Preserva a causa original (stack trace) para facilitar o debug em produção,
 * ao contrário de simplesmente relançar com a mensagem como texto.
 */
public class MessagePublishException extends RuntimeException {
  public MessagePublishException(String exchange, String routingKey, Throwable cause) {
    super("Failed to publish message to exchange '" + exchange + "' with routing key '" + routingKey + "'", cause);
  }
}
