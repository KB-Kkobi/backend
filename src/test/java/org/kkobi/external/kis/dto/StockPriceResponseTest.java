package org.kkobi.external.kis.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockPriceResponseTest {

    @Test
    @DisplayName("KIS 현재가 output을 StockPriceResponse로 정규화한다.")
    void mapKisOutputToRecord() {
        Map<String, String> output = new HashMap<>();
        output.put("hts_kor_isnm", "삼성전자");
        output.put("stck_prpr", "73000");
        output.put("prdy_vrss", "-500");
        output.put("prdy_ctrt", "-0.68");
        output.put("acml_vol", "12345678");
        output.put("stck_oprc", "73500");
        output.put("stck_hgpr", "73800");
        output.put("stck_lwpr", "72900");
        output.put("stck_sdpr", "73500");

        StockPriceResponse response = StockPriceResponse.fromKisOutput("005930", output);

        assertEquals("005930", response.stockCode());
        assertEquals("삼성전자", response.stockName());
        assertEquals(new BigDecimal("73000"), response.price());
        assertEquals(new BigDecimal("-500"), response.change());
        assertEquals(new BigDecimal("-0.68"), response.changeRate());
        assertEquals(12345678L, response.volume());
        assertEquals(new BigDecimal("73500"), response.open());
        assertEquals(new BigDecimal("73800"), response.high());
        assertEquals(new BigDecimal("72900"), response.low());
        assertEquals(new BigDecimal("73500"), response.prevClose());
    }

    @Test
    @DisplayName("output의 숫자 필드가 비어있으면 null로 변환한다.")
    void nullifyBlankFields() {
        Map<String, String> output = new HashMap<>();
        output.put("hts_kor_isnm", "테스트");
        output.put("stck_prpr", "");
        output.put("acml_vol", " ");

        StockPriceResponse response = StockPriceResponse.fromKisOutput("000000", output);

        assertNull(response.price());
        assertNull(response.volume());
    }

    @Test
    @DisplayName("output이 null이면 예외를 던진다.")
    void throwOnNullOutput() {
        assertThrows(IllegalArgumentException.class,
                () -> StockPriceResponse.fromKisOutput("005930", null));
    }
}
