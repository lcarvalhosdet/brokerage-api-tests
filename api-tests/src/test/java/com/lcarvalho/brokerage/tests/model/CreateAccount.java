package com.lcarvalho.brokerage.tests.model;

import java.math.BigDecimal;

public record CreateAccount(String ownerName, BigDecimal initialCash) {
}
