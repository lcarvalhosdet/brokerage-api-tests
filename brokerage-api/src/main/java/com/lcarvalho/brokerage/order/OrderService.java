package com.lcarvalho.brokerage.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.lcarvalho.brokerage.account.AccountRepository;
import com.lcarvalho.brokerage.common.BusinessRuleException;
import com.lcarvalho.brokerage.common.NotFoundException;
import com.lcarvalho.brokerage.instrument.Instrument;
import com.lcarvalho.brokerage.instrument.InstrumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final AccountRepository accounts;
    private final InstrumentRepository instruments;
    private final OrderRepository orders;
    private final OrderExecutor executor = new OrderExecutor();

    public OrderService(AccountRepository accounts, InstrumentRepository instruments, OrderRepository orders) {
        this.accounts = accounts;
        this.instruments = instruments;
        this.orders = orders;
    }

    /** Fills a market order. If any rule fails, the transaction rolls back and nothing changes. */
    @Transactional
    public OrderResponse place(PlaceOrderRequest request) {
        BigDecimal cashBalance = accounts.lockCashBalance(request.accountId())
                .orElseThrow(() -> new BusinessRuleException(
                        "UNKNOWN_ACCOUNT", "Account " + request.accountId() + " does not exist"));
        Instrument instrument = instruments.findBySymbol(request.symbol())
                .orElseThrow(() -> new BusinessRuleException(
                        "UNKNOWN_INSTRUMENT", "Instrument " + request.symbol() + " does not exist"));
        long position = accounts.findPositionQuantity(request.accountId(), instrument.symbol());

        OrderExecutor.Execution execution = executor.execute(
                request.side(), request.quantity(), instrument.price(), cashBalance, position);

        accounts.updateCashBalance(request.accountId(), execution.newCashBalance());
        accounts.savePosition(request.accountId(), instrument.symbol(), execution.newPosition());

        UUID orderId = UUID.randomUUID();
        orders.insert(orderId, request.accountId(), instrument.symbol(), request.side(), request.quantity(),
                instrument.price(), execution.total(), OrderStatus.FILLED);
        return orders.findById(orderId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id) {
        return orders.findById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listByAccount(UUID accountId) {
        if (!accounts.exists(accountId)) {
            throw new NotFoundException("ACCOUNT_NOT_FOUND", "Account " + accountId + " not found");
        }
        return orders.findByAccount(accountId);
    }
}
