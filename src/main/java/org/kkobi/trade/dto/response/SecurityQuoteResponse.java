package org.kkobi.trade.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class SecurityQuoteResponse {
    private Long securityId;
    private String ticker;
    private String name;
    private String market;
    private long currentPrice;
    private long previousClose;
    private long changeAmount;
    private double changeRate;
    @JsonProperty("isMarketOpen")
    private boolean isMarketOpen;
    private OffsetDateTime quotedAt;
}
