package org.kkobi.external.kis.controller;

import lombok.RequiredArgsConstructor;
import org.kkobi.external.kis.dto.CandleResponse;
import org.kkobi.external.kis.dto.StockPriceResponse;
import org.kkobi.external.kis.service.StockQuoteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockQuoteService stockQuoteService;

    @GetMapping("/{stockCode}/price")
    public StockPriceResponse getPrice(@PathVariable String stockCode) {
        return stockQuoteService.getCurrentPrice(stockCode);
    }

    @GetMapping("/{stockCode}/chart")
    public List<CandleResponse> getChart(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "D") String period,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyyMMdd") LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyyMMdd") LocalDate to) {

        return stockQuoteService.getDailyChart(stockCode, period, from, to);
    }
}
