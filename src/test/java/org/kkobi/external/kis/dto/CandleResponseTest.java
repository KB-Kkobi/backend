package org.kkobi.external.kis.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandleResponseTest {

    @Test
    @DisplayName("KIS 일봉 output2 항목을 CandleResponse로 변환한다.")
    void mapKisOutputToRecord() {
        Map<String, String> output = new HashMap<>();
        output.put("stck_bsop_date", "20250801");
        output.put("stck_oprc", "73500");
        output.put("stck_hgpr", "73800");
        output.put("stck_lwpr", "72900");
        output.put("stck_clpr", "73000");
        output.put("acml_vol", "12345678");

        Optional<CandleResponse> result = CandleResponse.fromKisOutput(output);

        assertTrue(result.isPresent());
        CandleResponse candle = result.get();
        assertEquals(LocalDate.of(2025, 8, 1), candle.date());
        assertEquals(new BigDecimal("73500"), candle.open());
        assertEquals(new BigDecimal("73800"), candle.high());
        assertEquals(new BigDecimal("72900"), candle.low());
        assertEquals(new BigDecimal("73000"), candle.close());
        assertEquals(12345678L, candle.volume());
    }

    @Test
    @DisplayName("stck_bsop_date가 비어있으면 빈 Optional을 반환한다.")
    void skipEmptyDate() {
        Map<String, String> output = new HashMap<>();
        output.put("stck_bsop_date", "");
        output.put("stck_clpr", "73000");

        assertFalse(CandleResponse.fromKisOutput(output).isPresent());
    }

    @Test
    @DisplayName("output이 null이면 빈 Optional을 반환한다.")
    void skipNullOutput() {
        assertFalse(CandleResponse.fromKisOutput(null).isPresent());
    }
}
