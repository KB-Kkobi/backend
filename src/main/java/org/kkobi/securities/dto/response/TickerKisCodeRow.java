package org.kkobi.securities.dto.response;

import lombok.Data;

// ticker와 kis_code 매핑 결과 (배치 조회용)
@Data
public class TickerKisCodeRow {
    private String ticker;
    private String kisCode;
}
