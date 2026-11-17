package com.lcarvalho.brokerage.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.lcarvalho.brokerage.common.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for the order rules: no Spring context and no database. */
class OrderExecutorTest {

    private static final BigDecimal PRICE = new BigDecimal("38.50");

    private final OrderExecutor executor = new OrderExecutor();

    @Test
    @DisplayName("A buy debits the order total from the cash and adds to the position")
    void buyDebitsCashAndAddsToPosition() {
        OrderExecutor.Execution execution = executor.execute(Side.BUY, 10, PRICE, new BigDecimal("1000.00"), 5);

        assertThat(execution.total()).isEqualByComparingTo("385.00");
        assertThat(execution.newCashBalance()).isEqualByComparingTo("615.00");
        assertThat(execution.newPosition()).isEqualTo(15);
    }

    @Test
    @DisplayName("A sell credits the order total to the cash and reduces the position")
    void sellCreditsCashAndReducesPosition() {
        OrderExecutor.Execution execution = executor.execute(Side.SELL, 4, PRICE, new BigDecimal("100.00"), 10);

        assertThat(execution.total()).isEqualByComparingTo("154.00");
        assertThat(execution.newCashBalance()).isEqualByComparingTo("254.00");
        assertThat(execution.newPosition()).isEqualTo(6);
    }

    @Test
    @DisplayName("Buying with exactly the cash available leaves the balance at zero")
    void buyWithExactCash() {
        OrderExecutor.Execution execution = executor.execute(Side.BUY, 2, PRICE, new BigDecimal("77.00"), 0);

        assertThat(execution.newCashBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(execution.newPosition()).isEqualTo(2);
    }

    @Test
    @DisplayName("Buying one cent above the cash available is rejected")
    void buyAboveCashIsRejected() {
        assertThatThrownBy(() -> executor.execute(Side.BUY, 2, PRICE, new BigDecimal("76.99"), 0))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("INSUFFICIENT_FUNDS"));
    }

    @Test
    @DisplayName("Selling the whole position leaves it at zero")
    void sellWholePosition() {
        OrderExecutor.Execution execution = executor.execute(Side.SELL, 3, PRICE, BigDecimal.ZERO, 3);

        assertThat(execution.newPosition()).isZero();
        assertThat(execution.newCashBalance()).isEqualByComparingTo("115.50");
    }

    @Test
    @DisplayName("Selling more than the position is rejected")
    void sellAbovePositionIsRejected() {
        assertThatThrownBy(() -> executor.execute(Side.SELL, 4, PRICE, BigDecimal.ZERO, 3))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("INSUFFICIENT_POSITION"));
    }

    @ParameterizedTest(name = "quantity {0}")
    @ValueSource(ints = {0, -1})
    @DisplayName("A quantity below one is refused")
    void nonPositiveQuantityIsRefused(int quantity) {
        assertThatThrownBy(() -> executor.execute(Side.BUY, quantity, PRICE, BigDecimal.TEN, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
