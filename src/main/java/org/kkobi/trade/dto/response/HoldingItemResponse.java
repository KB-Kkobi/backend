package org.kkobi.trade.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class HoldingItemResponse {
    private Long holdingSecurityId;
    private Long securityId;
    private String ticker;
    private String name;
    private String market;
    private int quantity;
    private int lockedQuantity;
    private int sellableQuantity;
    private long averagePrice;
    private long principalAmount;
    // 시세 조회 실패 시 null
    private Long currentPrice;
    private Long valuationAmount;
    private Long profit;
    private Double profitRate;
    private Long previousClose;
    private Long changeAmount;
    private Double changeRate;
}
