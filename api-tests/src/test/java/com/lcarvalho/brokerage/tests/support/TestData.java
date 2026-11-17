package com.lcarvalho.brokerage.tests.support;

import java.util.UUID;

/** Test data builders. Every test creates its own data, so tests never depend on each other. */
public final class TestData {

    private TestData() {
    }

    public static String ownerName() {
        return "QA Investor " + UUID.randomUUID().toString().substring(0, 8);
    }
}
