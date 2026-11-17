package com.lcarvalho.brokerage.tests.clients;

import static io.restassured.RestAssured.given;

import java.math.BigDecimal;

import com.lcarvalho.brokerage.tests.model.Account;
import com.lcarvalho.brokerage.tests.model.CreateAccount;
import com.lcarvalho.brokerage.tests.support.Specs;
import com.lcarvalho.brokerage.tests.support.TestData;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** Calls to /accounts. Methods that return a Response leave every check to the test. */
public class AccountsClient {

    private final RequestSpecification spec;

    public AccountsClient() {
        this(Specs.authenticated());
    }

    public AccountsClient(RequestSpecification spec) {
        this.spec = spec;
    }

    public Response create(Object body) {
        return given().spec(spec).body(body).when().post("/accounts");
    }

    public Response get(String id) {
        return given().spec(spec).when().get("/accounts/{id}", id);
    }

    public Response orders(String id) {
        return given().spec(spec).when().get("/accounts/{id}/orders", id);
    }

    /** Creates an account for a test, failing right away if the API refuses it. */
    public Account createAccount(BigDecimal initialCash) {
        return create(new CreateAccount(TestData.ownerName(), initialCash))
                .then().statusCode(201)
                .extract().as(Account.class);
    }

    public Account getAccount(String id) {
        return get(id)
                .then().statusCode(200)
                .extract().as(Account.class);
    }
}
