package helen.com.sagaorchestratorservice.controller;

import helen.com.sagaorchestratorservice.compensation.CompensationService;
import helen.com.sagaorchestratorservice.saga.persistence.SagaEventEntity;
import helen.com.sagaorchestratorservice.saga.persistence.SagaInstanceEntity;
import helen.com.sagaorchestratorservice.saga.persistence.SagaStepEntity;
import helen.com.sagaorchestratorservice.saga.repository.SagaEventRepository;
import helen.com.sagaorchestratorservice.saga.repository.SagaStepRepository;
import helen.com.sagaorchestratorservice.saga.state.SagaStatus;
import helen.com.sagaorchestratorservice.service.SagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sagas")
@RequiredArgsConstructor
@Tag(name = "Sagas", description = "Consulta e operação administrativa de sagas. O fluxo " +
        "principal é disparado e avançado via mensageria; os endpoints de escrita aqui " +
        "servem apenas para intervenção manual (saga travada / handler abandonado).")
@SecurityRequirement(name = "basicAuth")
public class SagaController {
    private final SagaService sagaService;
    private final CompensationService compensationService;
    private final SagaStepRepository stepRepository;
    private final SagaEventRepository eventRepository;

    @GetMapping
    @Operation(summary = "Lista sagas, opcionalmente filtradas por status, paginado")
    public ResponseEntity<Page<Map<String, Object>>> listSagas(
            @Parameter(description = "Filtra pelo status da saga (ex.: RUNNING, COMPENSATING)")
            @RequestParam(required = false) SagaStatus status,
            @Parameter(description = "Número da página (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SagaInstanceEntity> result = sagaService.listSagas(status, pageable);
        return ResponseEntity.ok(result.map(this::toResponse));
    }

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

    @GetMapping("/{sagaId}/events")
    @Operation(summary = "Lista o histórico bruto de eventos recebidos pela saga")
    public ResponseEntity<List<SagaEventEntity>> getEvents(
            @Parameter(description = "Id da saga") @PathVariable UUID sagaId) {
        return ResponseEntity.ok(eventRepository.findBySagaId(sagaId));
    }

    @PostMapping("/{sagaId}/compensate")
    @Operation(summary = "Força uma saga travada a entrar em compensação imediatamente",
            description = "Uso administrativo: dispensa a espera pelo timeout automático. " +
                    "Só é aceito se a saga ainda não estiver num estado terminal ou já compensando.")
    public ResponseEntity<Void> forceCompensation(@PathVariable UUID sagaId) {
        compensationService.forceCompensation(sagaId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{sagaId}/compensation/{handlerName}/retry")
    @Operation(summary = "Reexecuta manualmente um handler de compensação abandonado",
            description = "Uso administrativo: ignora o limite de retries automáticos " +
                    "(saga.max-retry) e força uma nova tentativa imediata.")
    public ResponseEntity<Void> retryCompensationHandler(
            @PathVariable UUID sagaId,
            @Parameter(description = "Nome do handler, ex.: STOCK_COMPENSATION") @PathVariable String handlerName) {
        SagaInstanceEntity saga = sagaService.findBySagaId(sagaId);
        compensationService.forceRetryHandler(handlerName, sagaId, saga.getOrderId(), saga.getCorrelationId());
        return ResponseEntity.accepted().build();
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
