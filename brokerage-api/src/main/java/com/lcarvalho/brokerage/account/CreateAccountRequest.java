package com.lcarvalho.brokerage.account;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String ownerName,
        @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal initialCash) {
}
