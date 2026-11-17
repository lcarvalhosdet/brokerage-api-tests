package com.lcarvalho.brokerage.tests.model;

import java.math.BigDecimal;

public record Instrument(String symbol, String name, BigDecimal price) {
}
