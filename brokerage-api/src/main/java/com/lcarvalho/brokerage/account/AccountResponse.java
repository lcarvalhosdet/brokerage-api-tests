package com.lcarvalho.brokerage.account;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String ownerName,
        BigDecimal cashBalance,
        List<PositionResponse> positions,
        OffsetDateTime createdAt) {
}
