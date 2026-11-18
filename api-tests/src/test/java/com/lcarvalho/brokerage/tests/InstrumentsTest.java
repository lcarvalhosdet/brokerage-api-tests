package com.lcarvalho.brokerage.tests;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;

import java.math.BigDecimal;

import com.lcarvalho.brokerage.tests.clients.InstrumentsClient;
import com.lcarvalho.brokerage.tests.support.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Market data")
@Feature("Instruments")
@DisplayName("Instruments")
class InstrumentsTest extends BaseApiTest {

    private final InstrumentsClient instruments = new InstrumentsClient();

    @Test
    @Tag("smoke")
    @DisplayName("GET /instruments lists the tradable instruments")
    void listsInstruments() {
        instruments.list()
                .then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/instrument-list.json"))
                .body("symbol", hasItems("PETR4", "VALE3", "ITUB4"));
    }

    @Test
    @DisplayName("GET /instruments/{symbol} returns one instrument with a positive price")
    void getsInstrumentBySymbol() {
        instruments.get("PETR4")
                .then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/instrument.json"))
                .body("symbol", equalTo("PETR4"))
                .body("price", greaterThan(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("GET /instruments/{symbol} returns 404 for an unknown symbol")
    void unknownSymbolReturns404() {
        instruments.get("XXXX9")
                .then().statusCode(404)
                .body(matchesJsonSchemaInClasspath("schemas/error.json"))
                .body("code", equalTo("INSTRUMENT_NOT_FOUND"));
    }
}
