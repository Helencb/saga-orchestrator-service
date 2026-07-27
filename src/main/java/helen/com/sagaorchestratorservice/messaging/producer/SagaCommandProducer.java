package helen.com.sagaorchestratorservice.messaging.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import helen.com.sagaorchestratorservice.exception.MessagePublishException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaCommandProducer {
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    public void publish(String topicArn, Object payload) {
        String message;
        try {
            message = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            // erro de serialização não é transiente - não faz sentido dar retry
            throw new MessagePublishException(topicArn, ex);
        }

        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(message)
                    .build();
            snsClient.publish(request);
        } catch (Exception ex) {
            log.error("Failed to publish to topic {}", topicArn, ex);
            // preserva a causa original (antes o stack trace era perdido aqui)
            throw new MessagePublishException(topicArn, ex);
        }
    }
}