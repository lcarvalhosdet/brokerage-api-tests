package com.lcarvalho.brokerage.instrument;

import java.util.List;

import com.lcarvalho.brokerage.common.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/instruments")
public class InstrumentController {

    private final InstrumentRepository instruments;

    public InstrumentController(InstrumentRepository instruments) {
        this.instruments = instruments;
    }

    @GetMapping
    public List<Instrument> list() {
        return instruments.findAll();
    }

    @GetMapping("/{symbol}")
    public Instrument get(@PathVariable String symbol) {
        return instruments.findBySymbol(symbol)
                .orElseThrow(() -> new NotFoundException(
                        "INSTRUMENT_NOT_FOUND", "Instrument " + symbol + " not found"));
    }
}
