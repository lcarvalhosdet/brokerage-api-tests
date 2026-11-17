package com.lcarvalho.brokerage.tests.model;

import java.math.BigDecimal;

public record Order(
        String id,
        String accountId,
        String symbol,
        String side,
        int quantity,
        BigDecimal price,
        BigDecimal total,
        String status,
        String createdAt) {
}
