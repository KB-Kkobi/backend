package org.kkobi.securities.dto.response;

import lombok.Data;
import org.kkobi.securities.enums.SecurityType;

import java.math.BigDecimal;

// 종목 목록의 개별 항목 응답
@Data
public class SecurityListItemResponse {

    private Long securityId;
    private String ticker;
    private String name;
    private SecurityType type;

    // KIS 실시간 시세 조회 가능 여부 (kis_code IS NOT NULL)
    private boolean kisSupported;

    // 추천도(맞춤도) 0~100, 소수점 1자리. 성향 진단 미완료 시 null
    private BigDecimal matchScore;

    // 최신 거래일 거래량. 시세 데이터 없으면 null
    private Long volume;

    // 전일 종가 대비 등락률(%), 소수점 2자리. 시세 데이터 없으면 null
    private BigDecimal changeRate;
}
