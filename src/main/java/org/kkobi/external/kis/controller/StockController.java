package org.kkobi.external.kis.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "주식", description = "주식 시세 조회 API")
public class StockController {

    private final StockQuoteService stockQuoteService;

    @Operation(
            summary = "주식 현재가 조회",
            description = "종목 코드를 기준으로 현재 주식 가격을 조회합니다."
    )
    @GetMapping("/{stockCode}/price")
    public StockPriceResponse getPrice(@PathVariable String stockCode) {
        return stockQuoteService.getCurrentPrice(stockCode);
    }

    @Operation(
            summary = "주식 차트 조회",
            description = "종목 코드를 기준으로 주식 차트 데이터를 조회합니다."
    )
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
