package helen.com.sagaorchestratorservice.statemachine;

import helen.com.sagaorchestratorservice.saga.state.OrderSagaState;
import helen.com.sagaorchestratorservice.saga.state.SagaEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SagaTransitionValidatorTest {

    private SagaTransitionValidator validator;

    @BeforeEach
    void setUp() {
        SagaTransitionRegistry registry = new SagaTransitionRegistry();
        registry.init();
        validator = new SagaTransitionValidator(registry);
    }

    @Test
    void happyPathAdvancesThroughAllStates() {
        OrderSagaState state = OrderSagaState.ORDER_CREATED;

        state = validator.validate(state, SagaEventType.ORDER_CREATED);
        assertThat(state).isEqualTo(OrderSagaState.STOCK_RESERVATION_PENDING);

        state = validator.validate(state, SagaEventType.STOCK_RESERVED);
        assertThat(state).isEqualTo(OrderSagaState.STOCK_RESERVED);

        state = validator.validate(state, SagaEventType.PAYMENT_PROCESSED);
        assertThat(state).isEqualTo(OrderSagaState.ORDER_CONFIRM_PENDING);

        state = validator.validate(state, SagaEventType.ORDER_CONFIRMED);
        assertThat(state).isEqualTo(OrderSagaState.COMPLETED);
    }

    @Test
    void invalidTransitionThrows() {
        assertThatThrownBy(() -> validator.validate(OrderSagaState.ORDER_CREATED, SagaEventType.ORDER_CONFIRMED))
                .isInstanceOf(SagaStateMachineException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderSagaState.class, names = {"COMPLETED", "FAILED", "COMPENSATED"})
    void timeoutIsNotRegisteredFromTerminalStates(OrderSagaState terminalState) {
        assertThatThrownBy(() -> validator.validate(terminalState, SagaEventType.SAGA_TIMEOUT))
                .isInstanceOf(SagaStateMachineException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderSagaState.class, names = {"ORDER_CREATED", "STOCK_RESERVED", "COMPENSATING"})
    void timeoutIsRegisteredFromNonTerminalStates(OrderSagaState nonTerminalState) {
        assertThat(validator.validate(nonTerminalState, SagaEventType.SAGA_TIMEOUT))
                .isEqualTo(OrderSagaState.FAILED);
    }

    @Test
    void manualCompensationIsNotRegisteredWhenAlreadyCompensating() {
        assertThatThrownBy(() -> validator.validate(OrderSagaState.COMPENSATING,
                SagaEventType.MANUAL_COMPENSATION_TRIGGERED))
                .isInstanceOf(SagaStateMachineException.class);
    }

    @Test
    void manualCompensationMovesRunningStateToCompensating() {
        assertThat(validator.validate(OrderSagaState.STOCK_RESERVED,
                SagaEventType.MANUAL_COMPENSATION_TRIGGERED))
                .isEqualTo(OrderSagaState.COMPENSATING);
    }
}
