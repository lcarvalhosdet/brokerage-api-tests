package com.lcarvalho.brokerage.instrument;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class InstrumentRepository {

    private static final RowMapper<Instrument> MAPPER = (rs, rowNum) ->
            new Instrument(rs.getString("symbol"), rs.getString("name"), rs.getBigDecimal("price"));

    private final JdbcClient jdbc;

    public InstrumentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Instrument> findAll() {
        return jdbc.sql("SELECT symbol, name, price FROM instruments ORDER BY symbol")
                .query(MAPPER)
                .list();
    }

    public Optional<Instrument> findBySymbol(String symbol) {
        return jdbc.sql("SELECT symbol, name, price FROM instruments WHERE symbol = :symbol")
                .param("symbol", symbol)
                .query(MAPPER)
                .optional();
    }
}
