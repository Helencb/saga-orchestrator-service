package helen.com.sagaorchestratorservice.saga.orchestrator;

import helen.com.sagaorchestratorservice.compensation.CompensationService;
import helen.com.sagaorchestratorservice.messaging.command.ConfirmOrderCommand;
import helen.com.sagaorchestratorservice.messaging.command.ProcessPaymentCommand;
import helen.com.sagaorchestratorservice.messaging.command.ReserveStockCommand;
import helen.com.sagaorchestratorservice.messaging.dto.PaymentPayload;
import helen.com.sagaorchestratorservice.messaging.event.*;
import helen.com.sagaorchestratorservice.messaging.producer.InventoryCommandProducer;
import helen.com.sagaorchestratorservice.messaging.producer.OrderCommandProducer;
import helen.com.sagaorchestratorservice.messaging.producer.PaymentCommandProducer;
import helen.com.sagaorchestratorservice.saga.persistence.SagaInstanceEntity;
import helen.com.sagaorchestratorservice.saga.state.SagaEventType;
import helen.com.sagaorchestratorservice.saga.state.SagaStatus;
import helen.com.sagaorchestratorservice.saga.state.SagaStepStatus;
import helen.com.sagaorchestratorservice.service.SagaEventService;
import helen.com.sagaorchestratorservice.service.SagaService;
import helen.com.sagaorchestratorservice.service.SagaStepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaOrchestrator {
    private final SagaService sagaService;
    private final SagaEventService eventService;
    private final SagaStepService stepService;
    private final InventoryCommandProducer inventoryProducer;
    private final PaymentCommandProducer paymentProducer;
    private final OrderCommandProducer orderProducer;
    private final CompensationService compensationService;

    private static final String MDC_SAGA_ID = "sagaId";
    private static final String MDC_CORRELATION_ID = "correlationId";

    public void handleOrderCreated(OrderCreatedEvent event) {
        withMdc(event.getSagaId(), event.getCorrelationId(), () -> {
            log.info("Starting saga: {}", event.getSagaId());

            if (sagaService.existsBySagaId(event.getSagaId())) {
                log.warn("Saga {} already exists, skipping (idempotency)", event.getSagaId());
                return;
            }

            sagaService.createSaga(
                    event.getSagaId(),
                    event.getOrder().getOrderId(),
                    event.getCorrelationId(),
                    event.getOrder().getCustomerId(),
                    event.getOrder().getTotalAmount());

            eventService.saveEvent(
                    event.getSagaId(),
                    SagaEventType.ORDER_CREATED,
                    event,
                    event.getCorrelationId());

            ReserveStockCommand command = ReserveStockCommand.builder()
                    .sagaId(event.getSagaId())
                    .correlationId(event.getCorrelationId())
                    .order(event.getOrder())
                    .build();

            inventoryProducer.reserveStock(command);

            sagaService.updateState(
                    event.getSagaId(),
                    SagaEventType.ORDER_CREATED,
                    SagaStatus.RUNNING);

            stepService.saveStep(
                    event.getSagaId(),
                    "RESERVE_STOCK",
                    SagaStepStatus.RUNNING,
                    null);
        });
    }

    public void handleStockReserved(StockReservedEvent event) {
        withMdc(event.getSagaId(), event.getCorrelationId(), () -> {
            log.info("Stock reserved for saga: {}", event.getSagaId());

            eventService.saveEvent(
                    event.getSagaId(),
                    SagaEventType.STOCK_RESERVED,
                    event,
                    event.getCorrelationId());

            sagaService.updateState(
                    event.getSagaId(),
                    SagaEventType.STOCK_RESERVED,
                    SagaStatus.RUNNING);

            stepService.saveStep(
                    event.getSagaId(),
                    "RESERVE_STOCK",
                    SagaStepStatus.SUCCESS,
                    null
            );

            SagaInstanceEntity saga = sagaService.findBySagaId(event.getSagaId());

            PaymentPayload payment = PaymentPayload.builder()
                    .orderId(event.getOrderId())
                    .customerId(saga.getCustomerId())
                    .amount(saga.getTotalAmount())
                    .build();

            ProcessPaymentCommand command = ProcessPaymentCommand.builder()
                    .sagaId(event.getSagaId())
                    .correlationId(event.getCorrelationId())
                    .payment(payment)
                    .build();

            paymentProducer.processPayment(command);

            stepService.saveStep(
                    event.getSagaId(),
                    "PROCESS_PAYMENT",
                    SagaStepStatus.RUNNING,
                    null);
        });
    }

    public void handleStockFailed(StockReservationFailedEvent event) {
        withMdc(event.getSagaId(), event.getCorrelationId(), () -> {
            log.error("Stock reservation failed for saga: {}", event.getSagaId());

            eventService.saveEvent(
                    event.getSagaId(),
                    SagaEventType.STOCK_RESERVATION_FAILED,
                    event,
                    event.getCorrelationId());

            sagaService.updateState(
                    event.getSagaId(),
                    SagaEventType.STOCK_RESERVATION_FAILED,
                    SagaStatus.FAILED);

            stepService.saveStep(
                    event.getSagaId(),
                    "RESERVE_STOCK",
                    SagaStepStatus.FAILED,
                    event.getReason());
        });
    }

    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        withMdc(event.getSagaId(), event.getCorrelationId(), () -> {
            log.info("Payment processed for saga: {}", event.getSagaId());

            eventService.saveEvent(
                    event.getSagaId(),
                    SagaEventType.PAYMENT_PROCESSED,
                    event,
                    event.getCorrelationId());

            sagaService.updateState(
                    event.getSagaId(),
                    SagaEventType.PAYMENT_PROCESSED,
                    SagaStatus.RUNNING);

            stepService.saveStep(
                    event.getSagaId(),
                    "PROCESS_PAYMENT",
                    SagaStepStatus.SUCCESS,
                    null);

            ConfirmOrderCommand command = ConfirmOrderCommand.builder()
                    .sagaId(event.getSagaId())
                    .orderId(event.getOrderId())
                    .correlationId(event.getCorrelationId())
                    .build();

            orderProducer.confirmOrder(command);

            stepService.saveStep(
                    event.getSagaId(),
                    "CONFIRM_ORDER",
                    SagaStepStatus.RUNNING,
                    null);
        });
    }

    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        withMdc(event.getSagaId(), event.getCorrelationId(), () -> {
            log.info("Order confirmed for saga: {}", event.getSagaId());

            eventService.saveEvent(
                    event.getSagaId(),
                    SagaEventType.ORDER_CONFIRMED,
                    event,
                    event.getCorrelationId());

            sagaService.updateState(
                    event.getSagaId(),
                    SagaEventType.ORDER_CONFIRMED,
                    SagaStatus.COMPLETED);

            stepService.saveStep(
                    event.getSagaId(),
                    "CONFIRM_ORDER",
                    SagaStepStatus.SUCCESS,
                    null
            );

            log.info("Saga {} completed successfully", event.getSagaId());
        });
    }

    public void handlePaymentFailed(PaymentFailedEvent event) {
        withMdc(event.getSagaId(), event.getCorrelationId(), () -> {
            log.error("Payment failed for saga: {}", event.getSagaId());

            eventService.saveEvent(
                    event.getSagaId(),
                    SagaEventType.PAYMENT_FAILED,
                    event,
                    event.getCorrelationId()
            );

            sagaService.updateState(
                    event.getSagaId(),
                    SagaEventType.PAYMENT_FAILED,
                    SagaStatus.COMPENSATING
            );

            stepService.saveStep(
                    event.getSagaId(),
                    "PROCESS_PAYMENT",
                    SagaStepStatus.FAILED,
                    event.getReason()
            );

            compensationService.startCompensation(
                    event.getSagaId(),
                    event.getOrderId(),
                    event.getCorrelationId()
            );
        });
    }

    public void handleStockReleased(StockReleasedEvent event) {
        withMdc(event.getSagaId(), event.getCorrelationId(), () -> {
            log.info("Stock released for saga: {}", event.getSagaId());

            eventService.saveEvent(
                    event.getSagaId(),
                    SagaEventType.STOCK_RELEASED,
                    event,
                    event.getCorrelationId()
            );

            stepService.saveStep(
                    event.getSagaId(),
                    "RELEASE_STOCK",
                    SagaStepStatus.COMPENSATED,
                    null
            );

            sagaService.markCompensationStepDone(event.getSagaId(), "STOCK");
        });
    }

    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        withMdc(event.getSagaId(), event.getCorrelationId(), () -> {
            log.info("Payment refunded for saga: {}", event.getSagaId());

            eventService.saveEvent(
                    event.getSagaId(),
                    SagaEventType.PAYMENT_REFUNDED,
                    event,
                    event.getCorrelationId()
            );

            stepService.saveStep(
                    event.getSagaId(),
                    "REFUND_PAYMENT",
                    SagaStepStatus.COMPENSATED,
                    null
            );

            sagaService.markCompensationStepDone(event.getSagaId(), "PAYMENT");
        });
    }

    public void handleOrderCancelled(OrderCancelledEvent event) {
        withMdc(event.getSagaId(), event.getCorrelationId(), () -> {
            log.info("Order cancelled for saga: {}", event.getSagaId());

            eventService.saveEvent(
                    event.getSagaId(),
                    SagaEventType.ORDER_CANCELLED,
                    event,
                    event.getCorrelationId()
            );

            stepService.saveStep(
                    event.getSagaId(),
                    "CANCEL_ORDER",
                    SagaStepStatus.COMPENSATED,
                    null
            );

            sagaService.markCompensationStepDone(event.getSagaId(), "ORDER");
        });
    }

    /**
     * Coloca sagaId/correlationId no MDC para que todo log dentro do handler
     * (inclusive dos services/producers chamados) saia com esses campos -
     * antes era impossível filtrar os logs de uma saga específica sem grep manual.
     */
    private void withMdc(UUID sagaId, String correlationId, Runnable action) {
        MDC.put(MDC_SAGA_ID, String.valueOf(sagaId));
        MDC.put(MDC_CORRELATION_ID, correlationId);
        try {
            action.run();
        } finally {
            MDC.remove(MDC_SAGA_ID);
            MDC.remove(MDC_CORRELATION_ID);
        }
    }
}