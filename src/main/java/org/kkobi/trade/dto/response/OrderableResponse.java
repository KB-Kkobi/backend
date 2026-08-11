package org.kkobi.trade.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderableResponse {
    private Long securityId;
    private long orderableCash;
    private int sellableQuantity;
    private long currentPrice;
    private int maxBuyQuantityAtMarket;
    @JsonProperty("isMarketOpen")
    private boolean isMarketOpen;
}
