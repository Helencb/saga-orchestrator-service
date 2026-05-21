package helen.com.sagaorchestratorservice.saga.state;

public enum OrderSagaState {
    ORDER_CREATED,

    STOCK_RESERVATION_PENDING,
    STOCK_RESERVED,
    STOCK_FAILED,

    PAYMENT_PENDING,
    PAYMENT_PROCESSED,
    PAYMENT_FAILED,

    ORDER_CONFIRM_PENDING,
    ORDER_CONFIRMED,

    COMPENSATING,
    COMPENSATED,

    COMPLETED,
    FAILED
}
