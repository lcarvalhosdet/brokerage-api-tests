package com.lcarvalho.brokerage.tests.clients;

import static io.restassured.RestAssured.given;

import com.lcarvalho.brokerage.tests.model.Order;
import com.lcarvalho.brokerage.tests.model.PlaceOrder;
import com.lcarvalho.brokerage.tests.support.Specs;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** Calls to /orders. */
public class OrdersClient {

    private final RequestSpecification spec;

    public OrdersClient() {
        this(Specs.authenticated());
    }

    public OrdersClient(RequestSpecification spec) {
        this.spec = spec;
    }

    public Response place(Object body) {
        return given().spec(spec).body(body).when().post("/orders");
    }

    public Response get(String id) {
        return given().spec(spec).when().get("/orders/{id}", id);
    }

    /** Places an order for a test, failing right away if it is not filled. */
    public Order placeOrder(String accountId, String symbol, String side, int quantity) {
        return place(new PlaceOrder(accountId, symbol, side, quantity))
                .then().statusCode(201)
                .extract().as(Order.class);
    }
}
