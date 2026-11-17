package com.lcarvalho.brokerage.instrument;

import java.math.BigDecimal;

/** A tradable asset and its current price. */
public record Instrument(String symbol, String name, BigDecimal price) {
}
