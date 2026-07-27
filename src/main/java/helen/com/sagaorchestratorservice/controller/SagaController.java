package helen.com.sagaorchestratorservice.controller;

import helen.com.sagaorchestratorservice.saga.persistence.SagaInstanceEntity;
import helen.com.sagaorchestratorservice.saga.persistence.SagaStepEntity;
import helen.com.sagaorchestratorservice.saga.repository.SagaStepRepository;
import helen.com.sagaorchestratorservice.service.SagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sagas")
@RequiredArgsConstructor
@Tag(name = "Sagas", description = "Consulta somente-leitura do estado das sagas. " +
        "O fluxo em si é disparado e avançado via mensageria, não por esta API.")
@SecurityRequirement(name = "basicAuth")
public class SagaController {
    private final SagaService sagaService;
    private final SagaStepRepository stepRepository;

    @GetMapping("/{sagaId}")
    @Operation(summary = "Busca uma saga pelo seu id")
    public ResponseEntity<Map<String, Object>> getSaga(@PathVariable UUID sagaId) {
        SagaInstanceEntity saga = sagaService.findBySagaId(sagaId);
        return ResponseEntity.ok(toResponse(saga));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Busca a saga associada a um pedido (orderId)")
    public ResponseEntity<Map<String, Object>> getSagaByOrder(@PathVariable UUID orderId){
        SagaInstanceEntity saga = sagaService.findByOrderId(orderId);
        return ResponseEntity.ok(toResponse(saga));
    }

    @GetMapping("/{sagaId}/steps")
    @Operation(summary = "Lista o histórico de steps (execução e compensação) de uma saga")
    public ResponseEntity<List<SagaStepEntity>> getSteps(
            @Parameter(description = "Id da saga") @PathVariable UUID sagaId) {
        List<SagaStepEntity> steps = stepRepository.findBySagaIdOrderByCreatedAtAsc(sagaId);
        return ResponseEntity.ok(steps);
    }

    private Map<String, Object> toResponse(SagaInstanceEntity saga) {
        return Map.of(
                "sagaId", saga.getSagaId(),
                "orderId", saga.getOrderId(),
                "status", saga.getStatus(),
                "currentState", saga.getCurrentState(),
                "correlationId", saga.getCorrelationId(),
                "createdAt", saga.getCreatedAt(),
                "updatedAt", saga.getUpdatedAt()
        );
    }
}
