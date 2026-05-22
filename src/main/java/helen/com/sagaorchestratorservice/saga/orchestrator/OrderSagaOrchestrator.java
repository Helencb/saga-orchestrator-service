package helen.com.sagaorchestratorservice.saga.orchestrator;

import helen.com.sagaorchestratorservice.compensation.CompensationService;
import helen.com.sagaorchestratorservice.messaging.command.ProcessPaymentCommand;
import helen.com.sagaorchestratorservice.messaging.command.ReleaseStockCommand;
import helen.com.sagaorchestratorservice.messaging.command.ReserveStockCommand;
import helen.com.sagaorchestratorservice.messaging.dto.PaymentPayload;
import helen.com.sagaorchestratorservice.messaging.event.*;
import helen.com.sagaorchestratorservice.messaging.producer.InventoryCommandProducer;
import helen.com.sagaorchestratorservice.messaging.producer.PaymentCommandProducer;
import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaEventType;
import helen.com.sagaorchestratorservice.saga.state.SagaStatus;
import helen.com.sagaorchestratorservice.saga.state.SagaStepStatus;
import helen.com.sagaorchestratorservice.service.SagaEventService;
import helen.com.sagaorchestratorservice.service.SagaService;
import helen.com.sagaorchestratorservice.service.SagaStepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaOrchestrator {
    private final SagaService sagaService;
    private final SagaEventService eventService;
    private final SagaStepService stepService;
    private final InventoryCommandProducer inventoryProducer;
    private final PaymentCommandProducer paymentProducer;
    private final CompensationService compensationService;

    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Starting saga: {}", event.getSagaId());

        sagaService.createSaga(
                event.getSagaId(),
                event.getOrder().getOrderId(),
                event.getCorrelationId());

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

        sagaService.updateState(event.getSagaId(),
                OrderSagaState.STOCK_RESERVATION_PENDING,
                SagaStatus.RUNNING);

        stepService.saveStep(
                event.getSagaId(),
                "RESERVE_STOCK",
                SagaStepStatus.RUNNING,
                null);
    }

    public void handleStockReserved(StockReservedEvent event) {
        log.info("Stock reserved: {}", event.getSagaId());

        eventService.saveEvent(
                event.getSagaId(),
                SagaEventType.STOCK_RESERVED,
                event,
                event.getCorrelationId());

        sagaService.updateState(
                event.getSagaId(),
                OrderSagaState.STOCK_RESERVED,
                SagaStatus.RUNNING);

        PaymentPayload payment = PaymentPayload.builder()
                .orderId(event.getOrderId())
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
    }
    public void handleStockFailed(StockReservationFailedEvent event) {
        log.error("Stock reservation failed: {}", event.getSagaId());

        eventService.saveEvent(
                event.getSagaId(),
                SagaEventType.STOCK_RESERVATION_FAILED,
                event,
                event.getCorrelationId());

        sagaService.updateState(
                event.getSagaId(),
                OrderSagaState.FAILED,
                SagaStatus.FAILED);

        stepService.saveStep(
                event.getSagaId(),
                "RESERVE_STOCK",
                SagaStepStatus.FAILED,
                event.getReason());
    }
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Payment processed: {}", event.getSagaId());

        eventService.saveEvent(
                event.getSagaId(),
                SagaEventType.PAYMENT_PROCESSED,
                event,
                event.getCorrelationId());

        sagaService.updateState(
                event.getSagaId(),
                OrderSagaState.COMPLETED,
                SagaStatus.COMPLETED);

        stepService.saveStep(
                event.getSagaId(),
                "PROCESS_PAYMENT",
                SagaStepStatus.SUCCESS,
                null);
    }
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.error("Payment failed: {}", event.getSagaId());

        eventService.saveEvent(
                event.getSagaId(),
                SagaEventType.PAYMENT_FAILED,
                event,
                event.getCorrelationId());

        sagaService.updateState(
                event.getSagaId(),
                OrderSagaState.COMPENSATING,
                SagaStatus.COMPENSATING);

        ReleaseStockCommand releaseCommand = ReleaseStockCommand.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .correlationId(event.getCorrelationId())
                .build();

        inventoryProducer.releaseStock(releaseCommand);

        stepService.saveStep(
                event.getSagaId(),
                "RELEASE_STOCK",
                SagaStepStatus.RUNNING,
                null
        );
    }

    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.error("Payment failed: {}", event.getSagaId());

        eventService.saveEvent(
                event.getSagaId(),
                SagaEventType.PAYMENT_FAILED,
                event,
                event.getCorrelationId()
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
    }
}
