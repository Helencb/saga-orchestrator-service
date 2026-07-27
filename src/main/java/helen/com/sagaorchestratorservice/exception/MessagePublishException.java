package helen.com.sagaorchestratorservice.exception;

/**
 * Lançada quando a publicação de uma mensagem no SNS falha.
 * Preserva a causa original (stack trace) para facilitar o debug em produção,
 * ao contrário de simplesmente relançar com a mensagem como texto.
 */
public class MessagePublishException extends RuntimeException {
  public MessagePublishException(String topicArn, Throwable cause) {
    super("Failed to publish message to topic: " + topicArn, cause);
  }
}