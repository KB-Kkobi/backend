package org.kkobi.securities.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

// 단일 종목의 시세 응답
public record QuoteItem(
        String ticker,
        String kisCode,
        boolean supported,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changeRate,
        BigDecimal prevClose,
        Instant asOf,
        String error
) {
    public static QuoteItem unsupported(String ticker) {
        return new QuoteItem(ticker, null, false, null, null, null, null, null, null);
    }

    public static QuoteItem success(String ticker, String kisCode,
                                    BigDecimal price, BigDecimal change,
                                    BigDecimal changeRate, BigDecimal prevClose,
                                    Instant asOf) {
        return new QuoteItem(ticker, kisCode, true, price, change, changeRate, prevClose, asOf, null);
    }

    public static QuoteItem failed(String ticker, String kisCode, String error) {
        return new QuoteItem(ticker, kisCode, true, null, null, null, null, null, error);
    }
}
