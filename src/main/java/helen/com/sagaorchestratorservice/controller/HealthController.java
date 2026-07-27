package helen.com.sagaorchestratorservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Verificação simples de disponibilidade do serviço")
public class HealthController {
    @GetMapping
    @Operation(summary = "Status do serviço", description = "Endpoint público, sem autenticação")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(
                Map.of(
                        "service", "saga-orchestrator-service",
                        "status", "UP",
                        "timestamp", LocalDateTime.now()
                ));
    }
}
