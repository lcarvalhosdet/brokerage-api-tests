package com.lcarvalho.brokerage.tests.model;

import java.math.BigDecimal;
import java.util.List;

public record Account(String id, String ownerName, BigDecimal cashBalance, List<Position> positions, String createdAt) {
}
