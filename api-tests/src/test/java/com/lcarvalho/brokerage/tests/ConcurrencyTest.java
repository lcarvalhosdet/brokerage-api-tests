package com.lcarvalho.brokerage.tests;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.lcarvalho.brokerage.tests.clients.AccountsClient;
import com.lcarvalho.brokerage.tests.clients.InstrumentsClient;
import com.lcarvalho.brokerage.tests.clients.OrdersClient;
import com.lcarvalho.brokerage.tests.model.Account;
import com.lcarvalho.brokerage.tests.model.PlaceOrder;
import com.lcarvalho.brokerage.tests.support.BaseApiTest;
import com.lcarvalho.brokerage.tests.support.Specs;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Epic("Trading")
@Feature("Concurrency")
@DisplayName("Concurrency")
class ConcurrencyTest extends BaseApiTest {

    private static final BigDecimal INITIAL_CASH = new BigDecimal("1000000.00");
    private static final List<String> SYMBOLS = List.of("PETR4", "VALE3");
    private static final int WRITERS = 4;
    private static final int READERS = 4;
    private static final Duration DURATION = Duration.ofSeconds(3);

    // The loop below sends thousands of requests. Attaching each one to the Allure report would bury it
    // and slow down the very load the test relies on, so these two clients attach nothing
    private final AccountsClient accounts = new AccountsClient(Specs.authenticatedWithoutAttachments());
    private final OrdersClient orders = new OrdersClient(Specs.authenticatedWithoutAttachments());
    private final InstrumentsClient instruments = new InstrumentsClient();

    @Test
    @DisplayName("Reads of an account during concurrent orders always show a consistent snapshot")
    void accountReadsStayConsistentDuringConcurrentOrders() throws Exception {
        // Prices never change in the simulator, so whatever orders are filled,
        // the cash plus the value of the positions must always add up to the initial cash
        Map<String, BigDecimal> prices = SYMBOLS.stream().collect(toMap(identity(), instruments::priceOf));
        String accountId = accounts.createAccount(INITIAL_CASH).id();

        AtomicInteger filledOrders = new AtomicInteger();
        AtomicInteger reads = new AtomicInteger();
        Queue<Account> inconsistentReads = new ConcurrentLinkedQueue<>();
        long end = System.nanoTime() + DURATION.toNanos();

        ExecutorService pool = Executors.newFixedThreadPool(WRITERS + READERS);
        try {
            List<Future<?>> tasks = new ArrayList<>();
            for (int i = 0; i < WRITERS; i++) {
                tasks.add(pool.submit(() -> {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    while (System.nanoTime() < end) {
                        PlaceOrder order = new PlaceOrder(accountId, SYMBOLS.get(random.nextInt(SYMBOLS.size())),
                                random.nextBoolean() ? "BUY" : "SELL", random.nextInt(1, 21));
                        int status = orders.place(order).statusCode();
                        // A sell above the position is refused with 422; any other status is a failure
                        assertThat(status).isIn(201, 422);
                        if (status == 201) {
                            filledOrders.incrementAndGet();
                        }
                    }
                }));
            }
            for (int i = 0; i < READERS; i++) {
                tasks.add(pool.submit(() -> {
                    while (System.nanoTime() < end) {
                        Account account = accounts.getAccount(accountId);
                        reads.incrementAndGet();
                        if (portfolioValue(account, prices).compareTo(INITIAL_CASH) != 0) {
                            inconsistentReads.add(account);
                        }
                    }
                }));
            }
            for (Future<?> task : tasks) {
                task.get();
            }
        } finally {
            pool.shutdownNow();
        }

        // In the report, a summary of the load stands in for the requests that were not attached
        Allure.addAttachment("Load summary", "text/plain", """
                Writers: %d, readers: %d, duration: %d s
                Orders filled: %d
                Account reads: %d, inconsistent: %d
                """.formatted(WRITERS, READERS, DURATION.toSeconds(),
                filledOrders.get(), reads.get(), inconsistentReads.size()));

        assertThat(filledOrders.get()).as("orders filled while the account was read").isPositive();
        assertThat(inconsistentReads.size())
                .as("reads (out of %d) where the cash plus the positions did not add up to the initial cash; "
                        + "first one: %s", reads.get(), inconsistentReads.peek())
                .isZero();
    }

    private static BigDecimal portfolioValue(Account account, Map<String, BigDecimal> prices) {
        return account.positions().stream()
                .map(position -> prices.get(position.symbol()).multiply(BigDecimal.valueOf(position.quantity())))
                .reduce(account.cashBalance(), BigDecimal::add);
    }
}
