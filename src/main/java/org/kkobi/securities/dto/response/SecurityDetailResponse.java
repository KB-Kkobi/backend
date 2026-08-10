package org.kkobi.securities.dto.response;

import lombok.Data;
import org.kkobi.securities.enums.SecurityType;

import java.math.BigDecimal;

// 종목 상세 조회 응답
@Data
public class SecurityDetailResponse {

    private Long securityId;
    private String ticker;
    private String name;
    private SecurityType type;

    // KIS 실시간 시세 조회 가능 여부 (kis_code IS NOT NULL)
    private boolean kisSupported;

    // 시장 구분 (예: KOSPI, KOSDAQ)
    private String market;

    // 섹터
    private String sector;

    // 시가총액
    private Long marketCap;

    // 연 변동성
    private BigDecimal volatility;

    // 최대 낙폭 (Maximum Drawdown, 음수 값)
    private BigDecimal maxDrawdown;

    // 일평균 변동폭
    private BigDecimal averageDailyMove;

    // 평균 거래량
    private Long averageVolume;
}
