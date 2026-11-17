package com.lcarvalho.brokerage.tests;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import com.lcarvalho.brokerage.tests.clients.AccountsClient;
import com.lcarvalho.brokerage.tests.model.Account;
import com.lcarvalho.brokerage.tests.model.CreateAccount;
import com.lcarvalho.brokerage.tests.support.BaseApiTest;
import com.lcarvalho.brokerage.tests.support.TestData;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Accounts")
class AccountsTest extends BaseApiTest {

    private final AccountsClient accounts = new AccountsClient();

    @Test
    @Tag("smoke")
    @DisplayName("POST /accounts creates an account with the initial cash and no positions")
    void createsAccount() {
        CreateAccount request = new CreateAccount(TestData.ownerName(), new BigDecimal("10000.00"));

        Response response = accounts.create(request);

        response.then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("schemas/account.json"));
        Account account = response.as(Account.class);
        assertThat(response.header("Location")).endsWith("/accounts/" + account.id());
        assertThat(account.ownerName()).isEqualTo(request.ownerName());
        assertThat(account.cashBalance()).isEqualByComparingTo("10000.00");
        assertThat(account.positions()).isEmpty();
    }

    @Test
    @DisplayName("GET /accounts/{id} returns the account that was created")
    void getsCreatedAccount() {
        Account created = accounts.createAccount(new BigDecimal("2500.50"));

        Account fetched = accounts.get(created.id())
                .then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/account.json"))
                .extract().as(Account.class);

        assertThat(fetched).isEqualTo(created);
    }

    @Test
    @DisplayName("An account can start with zero cash")
    void acceptsZeroInitialCash() {
        Account account = accounts.createAccount(new BigDecimal("0.00"));

        assertThat(account.cashBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    static Stream<Arguments> invalidAccounts() {
        BigDecimal cash = new BigDecimal("100.00");
        return Stream.of(
                arguments("missing owner name", new CreateAccount(null, cash), "ownerName"),
                arguments("blank owner name", new CreateAccount("   ", cash), "ownerName"),
                arguments("owner name over 100 characters", new CreateAccount("x".repeat(101), cash), "ownerName"),
                arguments("missing initial cash", new CreateAccount("QA", null), "initialCash"),
                arguments("negative initial cash", new CreateAccount("QA", new BigDecimal("-0.01")), "initialCash"),
                arguments("three decimal places", new CreateAccount("QA", new BigDecimal("10.001")), "initialCash"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidAccounts")
    @DisplayName("POST /accounts rejects an invalid payload and names the field")
    void rejectsInvalidAccount(String scenario, CreateAccount request, String invalidField) {
        accounts.create(request)
                .then().statusCode(400)
                .body(matchesJsonSchemaInClasspath("schemas/error.json"))
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("details.field", hasItem(invalidField));
    }

    @Test
    @DisplayName("GET /accounts/{id} returns 404 for an unknown account")
    void unknownAccountReturns404() {
        accounts.get(UUID.randomUUID().toString())
                .then().statusCode(404)
                .body(matchesJsonSchemaInClasspath("schemas/error.json"))
                .body("code", equalTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /accounts/{id} returns 400 when the id is not a UUID")
    void malformedIdReturns400() {
        accounts.get("not-a-uuid")
                .then().statusCode(400)
                .body("code", equalTo("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("GET /accounts/{id}/orders returns 404 for an unknown account")
    void ordersOfUnknownAccountReturns404() {
        accounts.orders(UUID.randomUUID().toString())
                .then().statusCode(404)
                .body("code", equalTo("ACCOUNT_NOT_FOUND"));
    }
}
