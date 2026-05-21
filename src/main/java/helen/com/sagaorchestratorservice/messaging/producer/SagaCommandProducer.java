package helen.com.sagaorchestratorservice.messaging.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
@RequiredArgsConstructor
public class SagaCommandProducer {
    private final SnsClient snsClient;

    private final ObjectMapper objectMapper;

    public void publish(String topicArn, Object payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(message)
                    .build();
            snsClient.publish(request);

        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }
}
