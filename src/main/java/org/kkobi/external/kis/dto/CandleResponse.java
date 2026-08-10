package org.kkobi.external.kis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

public record CandleResponse(
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        Long volume
) {
    private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    // KIS 응답 output2 항목을 CandleResponse로 변환한다.
    // 빈 객체(stck_bsop_date 누락)는 Optional.empty()로 반환한다.
    public static Optional<CandleResponse> fromKisOutput(Map<String, String> output) {
        if (output == null) {
            return Optional.empty();
        }
        String rawDate = output.get("stck_bsop_date");
        if (rawDate == null || rawDate.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new CandleResponse(
                LocalDate.parse(rawDate.trim(), KIS_DATE),
                parseDecimal(output.get("stck_oprc")),
                parseDecimal(output.get("stck_hgpr")),
                parseDecimal(output.get("stck_lwpr")),
                parseDecimal(output.get("stck_clpr")),
                parseLong(output.get("acml_vol"))
        ));
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
