package com.lcarvalho.brokerage.tests;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;

import com.lcarvalho.brokerage.tests.clients.InstrumentsClient;
import com.lcarvalho.brokerage.tests.support.BaseApiTest;
import com.lcarvalho.brokerage.tests.support.Specs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("Health check and authentication")
class HealthAndAuthTest extends BaseApiTest {

    @Test
    @Tag("smoke")
    @DisplayName("The health check is public and reports UP")
    void healthCheckIsPublic() {
        given().spec(Specs.anonymous())
                .when().get("/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @Tag("smoke")
    @DisplayName("A request without the API key gets 401")
    void requestWithoutApiKeyIsRejected() {
        new InstrumentsClient(Specs.anonymous()).list()
                .then().statusCode(401)
                .body(matchesJsonSchemaInClasspath("schemas/error.json"))
                .body("code", equalTo("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("A request with a wrong API key gets 401")
    void requestWithWrongApiKeyIsRejected() {
        new InstrumentsClient(Specs.withApiKey("wrong-key")).list()
                .then().statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"));
    }
}
