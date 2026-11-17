# Brokerage API: test automation with REST Assured

A personal project with two parts: a small brokerage simulator API and a black-box test suite for it, written in **Java with REST Assured and JUnit 5**. Every push runs the whole suite in GitHub Actions.

The API opens accounts, lists instruments and fills market orders. The test suite is the point of the project: contract checks, business rules, negative and boundary cases. Testing the API has already found [four real bugs](#bugs-found), each now guarded by a regression test. Next come data consistency, concurrency, real-time updates and load testing (see the [roadmap](#roadmap)).

## Stack

| Part | Tools |
| --- | --- |
| API tests | REST Assured 5, JUnit 5, AssertJ, JSON Schema validation |
| Unit tests | JUnit 5, AssertJ |
| Manual and exploratory checks | Postman collection, also runnable with Newman |
| API under test | Java 21, Spring Boot 3.5, PostgreSQL 16, Flyway |
| Infrastructure | Docker Compose, GitHub Actions |

## How it fits together

```
GitHub Actions
 ├── Unit tests ── order rules (no Spring, no database)
 └── API tests ─── REST Assured ── HTTP ──► brokerage-api (Spring Boot) ──► PostgreSQL
                                            └──────── Docker Compose ────────┘
```

```
brokerage-api/   the API under test (Spring Boot)
api-tests/       the black-box test suite (REST Assured + JUnit 5)
  clients/       one client per resource, so tests read like the business flow
  model/         request and response bodies as Java records
  support/       configuration, request specifications and test data
  resources/schemas/   JSON Schemas of every response
```

## Test strategy

- **Start from the risks.** In a brokerage, money and positions must never be created or lost. Most tests check balances and positions after each operation, not only the HTTP status.
- **Two layers.** Unit tests cover the order rules quickly and at every boundary. API tests treat the service as a black box and only talk to it over HTTP, as a client would.
- **Contracts.** Every response is checked against a JSON Schema with `additionalProperties: false`, so a renamed or unexpected field fails the build.
- **Negative and boundary cases.** Exact cash versus one cent short. Quantities of zero, negative, fractional and above the limit. Malformed ids, unknown resources, and a missing or wrong API key.
- **A rejected order changes nothing.** After a 422, the tests read the account again to prove that the balance, the positions and the order history did not change.
- **Consistent reads under load.** While orders are filled in parallel, every read of the account must add up. Prices are fixed in the simulator, so the cash plus the value of the positions must always equal the initial cash.
- **Independent tests.** Each test creates its own account, so tests run in any order and can run in parallel.
- **Exact money.** JSON numbers are read as `BigDecimal`, never as `double`, so amounts are compared without rounding errors.
- **Smoke and full runs.** Key tests carry `@Tag("smoke")` for a quick check of a new environment.

A subtle risk the suite guards: by default, Jackson silently turns `1.5` into `1` for an integer field, so an order for 1.5 shares would buy 1. The API disables that conversion, and a test makes sure `1.5` is rejected.

## Bugs found

The first version of the suite was green. Probing the running API with parallel requests and unusual inputs then found four bugs that it had missed. Each one now has a regression test that failed against the old API and passes against the fix.

1. **Inconsistent reads under load.** While orders were filled in parallel, 206 of 3,221 reads of `GET /accounts/{id}` broke the rule that the cash plus the value of the positions equals the initial cash. A read could show the cash from before an order next to the positions from after it, as if money had appeared or disappeared. The account and its positions were read in two queries under PostgreSQL's default `READ COMMITTED` level, where each query sees its own snapshot. The read now runs at `REPEATABLE READ`, so both queries see the same snapshot. Test: [`ConcurrencyTest`](api-tests/src/test/java/com/lcarvalho/brokerage/tests/ConcurrencyTest.java).
2. **A number read as the order side.** The suite guarded against `1.5` becoming `1` from the start, but by default Jackson also reads `"side": 1` (or `"1"`) as the second enum value, so the order became a sell. The API now rejects enum values sent as numbers. Test: `sideSentAsNumberIsRejected` in [`OrdersTest`](api-tests/src/test/java/com/lcarvalho/brokerage/tests/OrdersTest.java).
3. **Unknown fields ignored.** An order sent with `"orderType": "LIMIT", "limitPrice": 0.01` was filled at the market price, because Jackson ignored the fields it did not know. A client that believed it sent a limit order got a market order instead. The API now rejects unknown fields. Tests: `unknownFieldIsRejected` in `OrdersTest` and `rejectsUnknownField` in [`AccountsTest`](api-tests/src/test/java/com/lcarvalho/brokerage/tests/AccountsTest.java).
4. **A server error from one character.** A NUL character (`\u0000`) in the owner name or in the order symbol reached PostgreSQL, which cannot store it in a text column, and the API answered 500. The symbol must now match `[A-Z0-9]{1,10}`, and names cannot contain control characters. Tests: `controlCharacterInSymbolIsRejected` in `OrdersTest` and the control-character case in `AccountsTest`.

The same probes found no overdraft: 40 buys sent at the same time, with cash for exactly 10, filled exactly 10. After 12,052 concurrent orders, the balance still matched the order history to the cent. Turning these two checks into tests is on the roadmap.

## What is covered

| Area | Examples |
| --- | --- |
| Authentication | Public health check. 401 without the key or with a wrong key |
| Instruments | List and lookup, contract, unknown symbol |
| Accounts | Creation with `Location` header, lookup, validation of each field, control characters, unknown fields, unknown and malformed ids |
| Orders | Buy and sell update cash and positions. Exact-cash boundary. Insufficient funds and position leave the account unchanged. Payload validation, symbol format, fractional quantity, side sent as a number, unknown fields, order lookup and history |
| Concurrency | Every read of an account stays consistent while orders are filled in parallel |
| Order rules (unit) | Totals, balances and positions after buys and sells, and every rejection boundary |

## Running locally

You need Docker, Java 21 and Maven 3.9 or later.

```bash
docker compose up -d --build              # API on localhost:8080, PostgreSQL on localhost:5432
mvn -pl api-tests test                    # full API suite
mvn -pl api-tests test -Dgroups=smoke     # smoke tests only
mvn -pl brokerage-api -am test            # unit tests
docker compose down -v                    # stop and clean up
```

To test another environment, pass its address and key:

```bash
mvn -pl api-tests test -DbaseUrl=https://my-environment.example.com -DapiKey=my-key
```

## Postman collection

[`postman/brokerage-api.postman_collection.json`](postman/brokerage-api.postman_collection.json) covers every endpoint. The API key is set once, at collection level, and each request has checks on its response. Import it into Postman and run the "Happy path" requests in order: the ids of the new account and order are saved in collection variables automatically. The "Errors" folder shows the 401, 400, 404 and 422 responses.

To run the whole collection from the terminal:

```bash
npx newman run postman/brokerage-api.postman_collection.json
```

## The API

Every endpoint except the health check needs the `X-API-Key` header. Locally, the key is `local-dev-key`.

| Method | Path | What it does |
| --- | --- | --- |
| GET | `/actuator/health` | Health check (public) |
| GET | `/instruments` | Lists the tradable instruments |
| GET | `/instruments/{symbol}` | One instrument and its current price |
| POST | `/accounts` | Opens an account with initial cash |
| GET | `/accounts/{id}` | Cash balance and positions |
| GET | `/accounts/{id}/orders` | Order history, oldest first |
| POST | `/orders` | Places a market order, filled at the current price |
| GET | `/orders/{id}` | One order |

Every error has the same format:

```json
{
  "code": "INSUFFICIENT_FUNDS",
  "message": "Order total 385.00 is above the cash balance 100.00",
  "details": []
}
```

Validation errors return 400 and list each invalid field in `details`. Broken business rules return 422. Unknown resources in the path return 404.

Instrument prices are fictional: this is a simulator, not market data.

## Roadmap

- [x] Accounts, instruments and market orders, with REST tests and CI
- [ ] Idempotency keys: the same order sent twice is filled once, checked in the database
- [ ] Data consistency checks with direct PostgreSQL queries
- [ ] Concurrency: parallel orders must never overdraw an account
- [ ] WebSocket feed with prices and order updates
- [ ] Load test with k6 and failure-recovery tests
- [ ] Allure report published on GitHub Pages

## Author

Leandro Carvalho, QA engineer and SDET. [LinkedIn](https://www.linkedin.com/in/leandro-carvalho-sdet)
