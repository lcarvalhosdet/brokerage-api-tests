package com.lcarvalho.brokerage.order;

import java.math.BigDecimal;

import com.lcarvalho.brokerage.common.BusinessRuleException;

/**
 * Business rules of a market order. The class knows nothing about Spring or the
 * database, so the rules are covered by fast unit tests.
 */
public class OrderExecutor {

    /** A filled order: its total, and the account cash and position after it. */
    public record Execution(BigDecimal total, BigDecimal newCashBalance, long newPosition) {
    }

    public Execution execute(Side side, int quantity, BigDecimal price, BigDecimal cashBalance, long position) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));
        return switch (side) {
            case BUY -> {
                if (cashBalance.compareTo(total) < 0) {
                    throw new BusinessRuleException("INSUFFICIENT_FUNDS",
                            "Order total " + total + " is above the cash balance " + cashBalance);
                }
                yield new Execution(total, cashBalance.subtract(total), position + quantity);
            }
            case SELL -> {
                if (position < quantity) {
                    throw new BusinessRuleException("INSUFFICIENT_POSITION",
                            "Cannot sell " + quantity + " units with a position of " + position);
                }
                yield new Execution(total, cashBalance.add(total), position - quantity);
            }
        };
    }
}
