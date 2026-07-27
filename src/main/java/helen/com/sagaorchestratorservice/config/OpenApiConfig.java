package helen.com.sagaorchestratorservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH_SCHEME = "basicAuth";

    @Bean
    public OpenAPI sagaOrchestratorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Saga Orchestrator Service")
                        .description("Orquestração da saga distribuída order -> stock -> payment " +
                                "(compensação reversa em caso de falha). A API HTTP é somente leitura; " +
                                "o fluxo é disparado e avançado via mensageria (SQS/SNS).")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BASIC_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")));
    }
}
