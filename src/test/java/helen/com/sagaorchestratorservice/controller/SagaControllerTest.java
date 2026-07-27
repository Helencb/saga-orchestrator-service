package helen.com.sagaorchestratorservice.controller;

import helen.com.sagaorchestratorservice.compensation.CompensationService;
import helen.com.sagaorchestratorservice.config.SecurityConfig;
import helen.com.sagaorchestratorservice.exception.GlobalExceptionHandler;
import helen.com.sagaorchestratorservice.exception.SagaNotFoundException;
import helen.com.sagaorchestratorservice.saga.persistence.SagaInstanceEntity;
import helen.com.sagaorchestratorservice.saga.repository.SagaEventRepository;
import helen.com.sagaorchestratorservice.saga.repository.SagaStepRepository;
import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaStatus;
import helen.com.sagaorchestratorservice.service.SagaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SagaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "security.actuator.username=admin",
        "security.actuator.password=test-pass"
})
class SagaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SagaService sagaService;

    @MockBean
    private CompensationService compensationService;

    @MockBean
    private SagaStepRepository stepRepository;

    @MockBean
    private SagaEventRepository eventRepository;

    private static final UUID SAGA_ID = UUID.randomUUID();

    private SagaInstanceEntity aSaga() {
        return SagaInstanceEntity.builder()
                .sagaId(SAGA_ID)
                .orderId(UUID.randomUUID())
                .status(SagaStatus.RUNNING)
                .currentState(OrderSagaState.STOCK_RESERVED)
                .correlationId("corr-1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void gettingASagaWithoutCredentialsIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/sagas/{sagaId}", SAGA_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void gettingAnExistingSagaReturnsOk() throws Exception {
        when(sagaService.findBySagaId(SAGA_ID)).thenReturn(aSaga());

        mockMvc.perform(get("/api/v1/sagas/{sagaId}", SAGA_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void gettingAMissingSagaReturns404() throws Exception {
        when(sagaService.findBySagaId(SAGA_ID)).thenThrow(new SagaNotFoundException("Saga not found"));

        mockMvc.perform(get("/api/v1/sagas/{sagaId}", SAGA_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void forceCompensationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/sagas/{sagaId}/compensate", SAGA_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void forceCompensationIsAcceptedForAnAuthenticatedAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/sagas/{sagaId}/compensate", SAGA_ID))
                .andExpect(status().isAccepted());
    }

    @Test
    void authenticatingWithBasicAuthAsAdminIsAllowed() throws Exception {
        when(sagaService.findBySagaId(any())).thenReturn(aSaga());

        mockMvc.perform(get("/api/v1/sagas/{sagaId}", SAGA_ID).with(httpBasic("admin", "test-pass")))
                .andExpect(status().isOk());
    }

    @Test
    void trustingGatewayHeadersWithAdminRoleIsAllowed() throws Exception {
        when(sagaService.findBySagaId(any())).thenReturn(aSaga());

        mockMvc.perform(get("/api/v1/sagas/{sagaId}", SAGA_ID)
                        .header("X-User-Id", "user-123")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void trustingGatewayHeadersWithNonAdminRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/sagas/{sagaId}", SAGA_ID)
                        .header("X-User-Id", "user-123")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }
}
