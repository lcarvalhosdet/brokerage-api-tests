package com.lcarvalho.brokerage.tests.clients;

import static io.restassured.RestAssured.given;

import java.math.BigDecimal;

import com.lcarvalho.brokerage.tests.model.Instrument;
import com.lcarvalho.brokerage.tests.support.Specs;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** Calls to /instruments. */
public class InstrumentsClient {

    private final RequestSpecification spec;

    public InstrumentsClient() {
        this(Specs.authenticated());
    }

    public InstrumentsClient(RequestSpecification spec) {
        this.spec = spec;
    }

    public Response list() {
        return given().spec(spec).when().get("/instruments");
    }

    public Response get(String symbol) {
        return given().spec(spec).when().get("/instruments/{symbol}", symbol);
    }

    public BigDecimal priceOf(String symbol) {
        return get(symbol)
                .then().statusCode(200)
                .extract().as(Instrument.class)
                .price();
    }
}
