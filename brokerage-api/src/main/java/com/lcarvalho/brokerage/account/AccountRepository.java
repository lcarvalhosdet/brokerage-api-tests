package com.lcarvalho.brokerage.account;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

    /** An account row, without its positions. */
    public record AccountRow(UUID id, String ownerName, BigDecimal cashBalance, OffsetDateTime createdAt) {
    }

    private static final RowMapper<AccountRow> ACCOUNT = (rs, rowNum) -> new AccountRow(
            rs.getObject("id", UUID.class),
            rs.getString("owner_name"),
            rs.getBigDecimal("cash_balance"),
            rs.getObject("created_at", OffsetDateTime.class));

    private final JdbcClient jdbc;

    public AccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID id, String ownerName, BigDecimal cashBalance) {
        jdbc.sql("INSERT INTO accounts (id, owner_name, cash_balance) VALUES (:id, :ownerName, :cashBalance)")
                .param("id", id)
                .param("ownerName", ownerName)
                .param("cashBalance", cashBalance)
                .update();
    }

    public Optional<AccountRow> findById(UUID id) {
        return jdbc.sql("SELECT id, owner_name, cash_balance, created_at FROM accounts WHERE id = :id")
                .param("id", id)
                .query(ACCOUNT)
                .optional();
    }

    public boolean exists(UUID id) {
        Long count = jdbc.sql("SELECT COUNT(*) FROM accounts WHERE id = :id")
                .param("id", id)
                .query(Long.class)
                .single();
        return count > 0;
    }

    /**
     * Reads the cash balance and locks the account row until the transaction ends,
     * so two orders for the same account can never run at the same time.
     */
    public Optional<BigDecimal> lockCashBalance(UUID id) {
        return jdbc.sql("SELECT cash_balance FROM accounts WHERE id = :id FOR UPDATE")
                .param("id", id)
                .query(BigDecimal.class)
                .optional();
    }

    public void updateCashBalance(UUID id, BigDecimal cashBalance) {
        jdbc.sql("UPDATE accounts SET cash_balance = :cashBalance WHERE id = :id")
                .param("cashBalance", cashBalance)
                .param("id", id)
                .update();
    }

    public long findPositionQuantity(UUID accountId, String symbol) {
        return jdbc.sql("SELECT quantity FROM positions WHERE account_id = :accountId AND symbol = :symbol")
                .param("accountId", accountId)
                .param("symbol", symbol)
                .query(Long.class)
                .optional()
                .orElse(0L);
    }

    public void savePosition(UUID accountId, String symbol, long quantity) {
        jdbc.sql("""
                INSERT INTO positions (account_id, symbol, quantity)
                VALUES (:accountId, :symbol, :quantity)
                ON CONFLICT (account_id, symbol) DO UPDATE SET quantity = EXCLUDED.quantity
                """)
                .param("accountId", accountId)
                .param("symbol", symbol)
                .param("quantity", quantity)
                .update();
    }

    public List<PositionResponse> findPositions(UUID accountId) {
        return jdbc.sql("""
                SELECT symbol, quantity FROM positions
                WHERE account_id = :accountId AND quantity > 0
                ORDER BY symbol
                """)
                .param("accountId", accountId)
                .query((rs, rowNum) -> new PositionResponse(rs.getString("symbol"), rs.getLong("quantity")))
                .list();
    }
}
