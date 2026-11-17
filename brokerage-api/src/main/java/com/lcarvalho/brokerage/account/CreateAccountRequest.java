package com.lcarvalho.brokerage.account;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        // PostgreSQL cannot store a NUL character, so control characters are refused here
        @NotBlank @Size(max = 100)
        @Pattern(regexp = "[^\\p{Cntrl}]*", message = "must not contain control characters")
        String ownerName,
        @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal initialCash) {
}
