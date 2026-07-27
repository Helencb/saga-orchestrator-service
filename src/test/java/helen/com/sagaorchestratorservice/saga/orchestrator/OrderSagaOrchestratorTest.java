package helen.com.sagaorchestratorservice.saga.orchestrator;

import helen.com.sagaorchestratorservice.compensation.CompensationService;
import helen.com.sagaorchestratorservice.messaging.command.ProcessPaymentCommand;
import helen.com.sagaorchestratorservice.messaging.event.OrderCreatedEvent;
import helen.com.sagaorchestratorservice.messaging.event.StockReservedEvent;
import helen.com.sagaorchestratorservice.messaging.producer.InventoryCommandProducer;
import helen.com.sagaorchestratorservice.messaging.producer.OrderCommandProducer;
import helen.com.sagaorchestratorservice.messaging.producer.PaymentCommandProducer;
import helen.com.sagaorchestratorservice.saga.persistence.SagaInstanceEntity;
import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaStatus;
import helen.com.sagaorchestratorservice.service.SagaEventService;
import helen.com.sagaorchestratorservice.service.SagaService;
import helen.com.sagaorchestratorservice.service.SagaStepService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderSagaOrchestratorTest {

    @Mock private SagaService sagaService;
    @Mock private SagaEventService eventService;
    @Mock private SagaStepService stepService;
    @Mock private InventoryCommandProducer inventoryProducer;
    @Mock private PaymentCommandProducer paymentProducer;
    @Mock private OrderCommandProducer orderProducer;
    @Mock private CompensationService compensationService;

    private OrderSagaOrchestrator orchestrator;

    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orchestrator = new OrderSagaOrchestrator(sagaService, eventService, stepService,
                inventoryProducer, paymentProducer, orderProducer, compensationService);
    }

    @Test
    void handleOrderCreatedSkipsWhenSagaAlreadyExists() {
        when(sagaService.existsBySagaId(SAGA_ID)).thenReturn(true);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .sagaId(SAGA_ID)
                .correlationId("corr-1")
                .build();

        orchestrator.handleOrderCreated(event);

        verify(sagaService, never()).createSaga(any(), any(), any(), any(), any());
        verifyNoInteractions(inventoryProducer);
    }

    @Test
    void handleStockReservedBuildsPaymentPayloadWithCustomerAndAmountFromTheSaga() {
        BigDecimal amount = new BigDecimal("250.00");
        SagaInstanceEntity saga = SagaInstanceEntity.builder()
                .sagaId(SAGA_ID)
                .orderId(ORDER_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(amount)
                .status(SagaStatus.RUNNING)
                .currentState(OrderSagaState.STOCK_RESERVATION_PENDING)
                .correlationId("corr-1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(sagaService.findBySagaId(SAGA_ID)).thenReturn(saga);

        StockReservedEvent event = StockReservedEvent.builder()
                .sagaId(SAGA_ID)
                .orderId(ORDER_ID)
                .correlationId("corr-1")
                .build();

        orchestrator.handleStockReserved(event);

        ArgumentCaptor<ProcessPaymentCommand> captor = ArgumentCaptor.forClass(ProcessPaymentCommand.class);
        verify(paymentProducer).processPayment(captor.capture());

        ProcessPaymentCommand command = captor.getValue();
        assertThat(command.getPayment().getOrderId()).isEqualTo(ORDER_ID);
        assertThat(command.getPayment().getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(command.getPayment().getAmount()).isEqualTo(amount);
    }
}
