package org.kkobi.external.kis.realtime.dto;

import java.math.BigDecimal;

// KIS 실시간 체결가(H0STCNT0) 프레임을 프론트에서 사용할 필드만 정규화한 DTO
public record StockTick(
        String stockCode,
        String tradeTime,       // HHMMSS
        BigDecimal price,       // 현재 체결가
        BigDecimal change,      // 전일 대비
        BigDecimal changeRate,  // 전일 대비율 %
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        Long tradeVolume,       // 체결 거래량 (건별)
        Long cumulativeVolume   // 누적 거래량
) {}
