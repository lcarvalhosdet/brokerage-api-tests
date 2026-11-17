package com.lcarvalho.brokerage.tests.support;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

/** Same REST Assured defaults for requests that do not use a specification from {@link Specs}. */
public abstract class BaseApiTest {

    @BeforeAll
    static void configureRestAssured() {
        RestAssured.baseURI = ApiConfig.baseUrl();
        RestAssured.config = Specs.config();
    }
}
