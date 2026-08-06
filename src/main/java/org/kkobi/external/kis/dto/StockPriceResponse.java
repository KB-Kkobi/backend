package org.kkobi.external.kis.dto;

import java.math.BigDecimal;
import java.util.Map;

public record StockPriceResponse(
        String stockCode,
        String stockName,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changeRate,
        Long volume,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal prevClose
) {
    public static StockPriceResponse fromKisOutput(String stockCode, Map<String, String> output) {
        if (output == null) {
            throw new IllegalArgumentException("KIS 현재가 output이 비어 있습니다.");
        }
        return new StockPriceResponse(
                stockCode,
                output.getOrDefault("hts_kor_isnm", ""),
                parseDecimal(output.get("stck_prpr")),
                parseDecimal(output.get("prdy_vrss")),
                parseDecimal(output.get("prdy_ctrt")),
                parseLong(output.get("acml_vol")),
                parseDecimal(output.get("stck_oprc")),
                parseDecimal(output.get("stck_hgpr")),
                parseDecimal(output.get("stck_lwpr")),
                parseDecimal(output.get("stck_sdpr"))
        );
    }

    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.trim());
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value.trim());
    }
}
