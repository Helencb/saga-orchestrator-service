package helen.com.sagaorchestratorservice.util;

import java.util.UUID;

public class CorrelationIdUtil {
    private CorrelationIdUtil() {}

    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
