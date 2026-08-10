package org.kkobi.external.kis.realtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.external.kis.realtime.dto.StockTick;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KisTickFrameParserTest {

    private final KisTickFrameParser parser = new KisTickFrameParser();

    @Test
    @DisplayName("H0STCNT0 단일 레코드 파이프 프레임을 StockTick으로 파싱한다.")
    void parseSingleRecord() {
        String frame = "0|H0STCNT0|001|" + String.join("^",
                "005930",   // 0 MKSC_SHRN_ISCD
                "123045",   // 1 STCK_CNTG_HOUR
                "73000",    // 2 STCK_PRPR
                "5",        // 3 PRDY_VRSS_SIGN
                "-500",     // 4 PRDY_VRSS
                "-0.68",    // 5 PRDY_CTRT
                "73200",    // 6 WGHN_AVRG_STCK_PRC
                "73500",    // 7 STCK_OPRC
                "73800",    // 8 STCK_HGPR
                "72900",    // 9 STCK_LWPR
                "73100",    // 10 ASKP1
                "73000",    // 11 BIDP1
                "100",      // 12 CNTG_VOL
                "12345678"  // 13 ACML_VOL
        );

        List<StockTick> ticks = parser.parse(frame);

        assertEquals(1, ticks.size());
        StockTick tick = ticks.get(0);
        assertEquals("005930", tick.stockCode());
        assertEquals("123045", tick.tradeTime());
        assertEquals(new BigDecimal("73000"), tick.price());
        assertEquals(new BigDecimal("-500"), tick.change());
        assertEquals(new BigDecimal("-0.68"), tick.changeRate());
        assertEquals(new BigDecimal("73500"), tick.open());
        assertEquals(new BigDecimal("73800"), tick.high());
        assertEquals(new BigDecimal("72900"), tick.low());
        assertEquals(100L, tick.tradeVolume());
        assertEquals(12345678L, tick.cumulativeVolume());
    }

    @Test
    @DisplayName("다중 레코드는 '+' 구분자로 분리해 여러 StockTick으로 파싱한다.")
    void parseMultipleRecords() {
        String record1 = String.join("^",
                "005930", "123045", "73000", "5", "-500", "-0.68",
                "0", "73500", "73800", "72900", "0", "0", "100", "12345678");
        String record2 = String.join("^",
                "000660", "123046", "180000", "2", "3000", "1.69",
                "0", "178000", "181000", "177500", "0", "0", "50", "8765432");
        String frame = "0|H0STCNT0|002|" + record1 + "+" + record2;

        List<StockTick> ticks = parser.parse(frame);

        assertEquals(2, ticks.size());
        assertEquals("005930", ticks.get(0).stockCode());
        assertEquals("000660", ticks.get(1).stockCode());
        assertEquals(new BigDecimal("180000"), ticks.get(1).price());
    }

    @Test
    @DisplayName("JSON 프레임은 tick 프레임이 아니라고 판정한다.")
    void detectNonTickFrame() {
        String json = "{\"header\":{\"tr_id\":\"PINGPONG\"}}";
        assertFalse(parser.isTickFrame(json));
        assertTrue(parser.parse(json).isEmpty());
    }

    @Test
    @DisplayName("필드가 부족한 잘못된 레코드는 무시한다.")
    void skipMalformedRecord() {
        String frame = "0|H0STCNT0|001|005930^123045^73000";
        List<StockTick> ticks = parser.parse(frame);
        assertNotNull(ticks);
        assertTrue(ticks.isEmpty());
    }
}
