package com.lcarvalho.brokerage.tests.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.JsonConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.specification.RequestSpecification;

/** Request specifications shared by every client. */
public final class Specs {

    public static final String API_KEY_HEADER = "X-API-Key";

    private Specs() {
    }

    /** Requests that carry the valid API key. */
    public static RequestSpecification authenticated() {
        return withApiKey(ApiConfig.apiKey());
    }

    public static RequestSpecification withApiKey(String apiKey) {
        return base().addHeader(API_KEY_HEADER, apiKey).build();
    }

    /** Requests without any API key. */
    public static RequestSpecification anonymous() {
        return base().build();
    }

    public static RestAssuredConfig config() {
        return RestAssuredConfig.config()
                // Read decimal JSON numbers as BigDecimal, so money is compared without float rounding
                .jsonConfig(JsonConfig.jsonConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL))
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> new ObjectMapper()
                                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)))
                // Print the full request and response only when a check fails
                .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails());
    }

    private static RequestSpecBuilder base() {
        return new RequestSpecBuilder()
                .setConfig(config())
                .setBaseUri(ApiConfig.baseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON);
    }
}
