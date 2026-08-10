package org.kkobi.securities.dto.response;

import lombok.Data;
import org.kkobi.securities.enums.SecurityType;

// 종목 목록의 개별 항목 응답
@Data
public class SecurityListItemResponse {

    private Long securityId;
    private String ticker;
    private String name;
    private SecurityType type;

    // KIS 실시간 시세 조회 가능 여부 (kis_code IS NOT NULL)
    private boolean kisSupported;
}
