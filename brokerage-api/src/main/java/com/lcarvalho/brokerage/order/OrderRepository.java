package com.lcarvalho.brokerage.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private static final String COLUMNS =
            "id, account_id, symbol, side, quantity, price, total, status, created_at";

    private static final RowMapper<OrderResponse> MAPPER = (rs, rowNum) -> new OrderResponse(
            rs.getObject("id", UUID.class),
            rs.getObject("account_id", UUID.class),
            rs.getString("symbol"),
            Side.valueOf(rs.getString("side")),
            rs.getInt("quantity"),
            rs.getBigDecimal("price"),
            rs.getBigDecimal("total"),
            OrderStatus.valueOf(rs.getString("status")),
            rs.getObject("created_at", OffsetDateTime.class));

    private final JdbcClient jdbc;

    public OrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID id, UUID accountId, String symbol, Side side, int quantity,
                       BigDecimal price, BigDecimal total, OrderStatus status) {
        jdbc.sql("""
                INSERT INTO orders (id, account_id, symbol, side, quantity, price, total, status)
                VALUES (:id, :accountId, :symbol, :side, :quantity, :price, :total, :status)
                """)
                .param("id", id)
                .param("accountId", accountId)
                .param("symbol", symbol)
                .param("side", side.name())
                .param("quantity", quantity)
                .param("price", price)
                .param("total", total)
                .param("status", status.name())
                .update();
    }

    public Optional<OrderResponse> findById(UUID id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM orders WHERE id = :id")
                .param("id", id)
                .query(MAPPER)
                .optional();
    }

    public List<OrderResponse> findByAccount(UUID accountId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM orders WHERE account_id = :accountId ORDER BY created_at, id")
                .param("accountId", accountId)
                .query(MAPPER)
                .list();
    }
}
