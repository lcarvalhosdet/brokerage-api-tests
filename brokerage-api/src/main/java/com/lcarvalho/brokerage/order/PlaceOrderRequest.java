package com.lcarvalho.brokerage.order;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** A market order: it is filled at the current price of the instrument. */
public record PlaceOrderRequest(
        @NotNull UUID accountId,
        // Checking the format here keeps characters such as NUL away from the database
        @NotBlank @Pattern(regexp = "[A-Z0-9]{1,10}", message = "must be 1 to 10 uppercase letters or digits")
        String symbol,
        @NotNull Side side,
        @NotNull @Positive @Max(1_000_000) Integer quantity) {
}
