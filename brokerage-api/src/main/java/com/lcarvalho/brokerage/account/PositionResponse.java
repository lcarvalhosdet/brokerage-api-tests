package com.lcarvalho.brokerage.account;

/** How many units of an instrument the account holds. */
public record PositionResponse(String symbol, long quantity) {
}
