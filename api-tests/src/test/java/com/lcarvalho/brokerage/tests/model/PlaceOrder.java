package com.lcarvalho.brokerage.tests.model;

public record PlaceOrder(String accountId, String symbol, String side, Integer quantity) {
}
