package com.lcarvalho.brokerage.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID accountId,
        String symbol,
        Side side,
        int quantity,
        BigDecimal price,
        BigDecimal total,
        OrderStatus status,
        OffsetDateTime createdAt) {
}
