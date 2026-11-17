package com.lcarvalho.brokerage.tests;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.lcarvalho.brokerage.tests.clients.AccountsClient;
import com.lcarvalho.brokerage.tests.clients.InstrumentsClient;
import com.lcarvalho.brokerage.tests.clients.OrdersClient;
import com.lcarvalho.brokerage.tests.model.Account;
import com.lcarvalho.brokerage.tests.model.Order;
import com.lcarvalho.brokerage.tests.model.PlaceOrder;
import com.lcarvalho.brokerage.tests.model.Position;
import com.lcarvalho.brokerage.tests.support.BaseApiTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Orders")
class OrdersTest extends BaseApiTest {

    private static final String SYMBOL = "PETR4";
    private static final BigDecimal INITIAL_CASH = new BigDecimal("10000.00");

    private final AccountsClient accounts = new AccountsClient();
    private final OrdersClient orders = new OrdersClient();
    private final InstrumentsClient instruments = new InstrumentsClient();

    @Test
    @Tag("smoke")
    @DisplayName("A buy order is filled at the current price and moves cash into the position")
    void buyOrderIsFilled() {
        BigDecimal price = instruments.priceOf(SYMBOL);
        Account account = accounts.createAccount(INITIAL_CASH);

        Response response = orders.place(new PlaceOrder(account.id(), SYMBOL, "BUY", 10));

        response.then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("schemas/order.json"));
        Order order = response.as(Order.class);
        assertThat(response.header("Location")).endsWith("/orders/" + order.id());
        assertThat(order.status()).isEqualTo("FILLED");
        assertThat(order.price()).isEqualByComparingTo(price);
        assertThat(order.total()).isEqualByComparingTo(order.price().multiply(BigDecimal.TEN));

        Account after = accounts.getAccount(account.id());
        assertThat(after.cashBalance()).isEqualByComparingTo(INITIAL_CASH.subtract(order.total()));
        assertThat(after.positions()).containsExactly(new Position(SYMBOL, 10));
    }

    @Test
    @DisplayName("A sell order credits the cash and reduces the position")
    void sellOrderIsFilled() {
        Account account = accounts.createAccount(INITIAL_CASH);
        Order buy = orders.placeOrder(account.id(), SYMBOL, "BUY", 10);

        Order sell = orders.placeOrder(account.id(), SYMBOL, "SELL", 4);

        Account after = accounts.getAccount(account.id());
        assertThat(after.cashBalance())
                .isEqualByComparingTo(INITIAL_CASH.subtract(buy.total()).add(sell.total()));
        assertThat(after.positions()).containsExactly(new Position(SYMBOL, 6));
    }

    @Test
    @DisplayName("Selling the whole position removes it from the account")
    void sellingWholePositionRemovesIt() {
        Account account = accounts.createAccount(INITIAL_CASH);
        orders.placeOrder(account.id(), SYMBOL, "BUY", 5);

        orders.placeOrder(account.id(), SYMBOL, "SELL", 5);

        assertThat(accounts.getAccount(account.id()).positions()).isEmpty();
    }

    @Test
    @DisplayName("Buying with exactly the available cash leaves the balance at zero")
    void buyWithExactCash() {
        BigDecimal price = instruments.priceOf(SYMBOL);
        Account account = accounts.createAccount(price.multiply(BigDecimal.valueOf(5)));

        orders.placeOrder(account.id(), SYMBOL, "BUY", 5);

        Account after = accounts.getAccount(account.id());
        assertThat(after.cashBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(after.positions()).containsExactly(new Position(SYMBOL, 5));
    }

    @Test
    @DisplayName("A buy order one cent above the available cash is rejected and changes nothing")
    void insufficientFundsChangesNothing() {
        BigDecimal price = instruments.priceOf(SYMBOL);
        BigDecimal cash = price.multiply(BigDecimal.valueOf(5)).subtract(new BigDecimal("0.01"));
        Account account = accounts.createAccount(cash);

        orders.place(new PlaceOrder(account.id(), SYMBOL, "BUY", 5))
                .then().statusCode(422)
                .body(matchesJsonSchemaInClasspath("schemas/error.json"))
                .body("code", equalTo("INSUFFICIENT_FUNDS"));

        Account after = accounts.getAccount(account.id());
        assertThat(after.cashBalance()).isEqualByComparingTo(cash);
        assertThat(after.positions()).isEmpty();
        accounts.orders(account.id())
                .then().statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @DisplayName("Selling more than the position is rejected and changes nothing")
    void insufficientPositionChangesNothing() {
        Account account = accounts.createAccount(INITIAL_CASH);
        orders.placeOrder(account.id(), SYMBOL, "BUY", 2);
        Account before = accounts.getAccount(account.id());

        orders.place(new PlaceOrder(account.id(), SYMBOL, "SELL", 3))
                .then().statusCode(422)
                .body("code", equalTo("INSUFFICIENT_POSITION"));

        assertThat(accounts.getAccount(account.id())).isEqualTo(before);
    }

    @Test
    @DisplayName("An order for an unknown instrument is rejected")
    void unknownInstrumentIsRejected() {
        Account account = accounts.createAccount(INITIAL_CASH);

        orders.place(new PlaceOrder(account.id(), "XXXX9", "BUY", 1))
                .then().statusCode(422)
                .body("code", equalTo("UNKNOWN_INSTRUMENT"));
    }

    @Test
    @DisplayName("An order for an unknown account is rejected")
    void unknownAccountIsRejected() {
        orders.place(new PlaceOrder(UUID.randomUUID().toString(), SYMBOL, "BUY", 1))
                .then().statusCode(422)
                .body("code", equalTo("UNKNOWN_ACCOUNT"));
    }

    static Stream<Arguments> invalidOrders() {
        // Payload checks run before the account lookup, so a random account id is enough here
        String accountId = UUID.randomUUID().toString();
        return Stream.of(
                arguments("zero quantity", new PlaceOrder(accountId, SYMBOL, "BUY", 0), "VALIDATION_ERROR"),
                arguments("negative quantity", new PlaceOrder(accountId, SYMBOL, "BUY", -1), "VALIDATION_ERROR"),
                arguments("quantity above the limit",
                        new PlaceOrder(accountId, SYMBOL, "BUY", 1_000_001), "VALIDATION_ERROR"),
                arguments("missing quantity", new PlaceOrder(accountId, SYMBOL, "BUY", null), "VALIDATION_ERROR"),
                arguments("missing symbol", new PlaceOrder(accountId, null, "BUY", 1), "VALIDATION_ERROR"),
                arguments("missing side", new PlaceOrder(accountId, SYMBOL, null, 1), "VALIDATION_ERROR"),
                arguments("missing account", new PlaceOrder(null, SYMBOL, "BUY", 1), "VALIDATION_ERROR"),
                arguments("unknown side", new PlaceOrder(accountId, SYMBOL, "HOLD", 1), "MALFORMED_REQUEST"),
                arguments("account id that is not a UUID",
                        new PlaceOrder("not-a-uuid", SYMBOL, "BUY", 1), "MALFORMED_REQUEST"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidOrders")
    @DisplayName("POST /orders rejects an invalid payload with 400")
    void rejectsInvalidOrder(String scenario, PlaceOrder request, String expectedCode) {
        orders.place(request)
                .then().statusCode(400)
                .body(matchesJsonSchemaInClasspath("schemas/error.json"))
                .body("code", equalTo(expectedCode));
    }

    @Test
    @DisplayName("A fractional quantity is rejected instead of being truncated")
    void fractionalQuantityIsRejected() {
        Account account = accounts.createAccount(INITIAL_CASH);

        orders.place(Map.of("accountId", account.id(), "symbol", SYMBOL, "side", "BUY", "quantity", 1.5))
                .then().statusCode(400)
                .body("code", equalTo("MALFORMED_REQUEST"));

        assertThat(accounts.getAccount(account.id()).positions()).isEmpty();
    }

    @Test
    @DisplayName("GET /orders/{id} returns the order that was placed")
    void getsOrderById() {
        Account account = accounts.createAccount(INITIAL_CASH);
        Order placed = orders.placeOrder(account.id(), SYMBOL, "BUY", 1);

        Order fetched = orders.get(placed.id())
                .then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/order.json"))
                .extract().as(Order.class);

        assertThat(fetched).isEqualTo(placed);
    }

    @Test
    @DisplayName("GET /orders/{id} returns 404 for an unknown order")
    void unknownOrderReturns404() {
        orders.get(UUID.randomUUID().toString())
                .then().statusCode(404)
                .body("code", equalTo("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /accounts/{id}/orders lists the orders in the order they were placed")
    void listsAccountOrdersInPlacementOrder() {
        Account account = accounts.createAccount(INITIAL_CASH);
        Order buy = orders.placeOrder(account.id(), SYMBOL, "BUY", 3);
        Order sell = orders.placeOrder(account.id(), SYMBOL, "SELL", 1);

        Order[] listed = accounts.orders(account.id())
                .then().statusCode(200)
                .extract().as(Order[].class);

        assertThat(listed).extracting(Order::id).containsExactly(buy.id(), sell.id());
    }
}
