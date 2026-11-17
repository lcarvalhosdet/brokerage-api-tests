package com.lcarvalho.brokerage.order;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** A market order: it is filled at the current price of the instrument. */
public record PlaceOrderRequest(
        @NotNull UUID accountId,
        @NotBlank @Size(max = 10) String symbol,
        @NotNull Side side,
        @NotNull @Positive @Max(1_000_000) Integer quantity) {
}
