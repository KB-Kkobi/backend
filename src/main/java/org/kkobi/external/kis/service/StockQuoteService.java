package org.kkobi.external.kis.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.external.kis.client.KisApiClient;
import org.kkobi.external.kis.dto.CandleResponse;
import org.kkobi.external.kis.dto.StockPriceResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockQuoteService {

    private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KisApiClient kisApiClient;

    public StockPriceResponse getCurrentPrice(String stockCode) {
        String code = normalizeStockCode(stockCode);
        Map<String, String> output = kisApiClient.fetchCurrentPrice(code);
        return StockPriceResponse.fromKisOutput(code, output);
    }

    public List<CandleResponse> getDailyChart(
            String stockCode, String period, LocalDate from, LocalDate to) {

        String code = normalizeStockCode(stockCode);
        String periodCode = normalizePeriod(period);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusMonths(3);
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("from은 to보다 이후일 수 없습니다.");
        }

        List<Map<String, String>> raw = kisApiClient.fetchDailyChart(
                code, start.format(KIS_DATE), end.format(KIS_DATE), periodCode);

        return raw.stream()
                .map(CandleResponse::fromKisOutput)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(CandleResponse::date))
                .toList();
    }

    private static String normalizeStockCode(String stockCode) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("종목 코드는 필수입니다.");
        }
        String trimmed = stockCode.trim();
        if (!trimmed.matches("\\d{6}")) {
            throw new IllegalArgumentException("종목 코드는 6자리 숫자여야 합니다.");
        }
        return trimmed;
    }

    private static String normalizePeriod(String period) {
        String value = period == null || period.isBlank() ? "D" : period.trim().toUpperCase();
        return switch (value) {
            case "D", "W", "M" -> value;
            default -> throw new IllegalArgumentException("period는 D, W, M 중 하나여야 합니다.");
        };
    }
}
