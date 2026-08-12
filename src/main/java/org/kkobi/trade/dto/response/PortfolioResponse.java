package org.kkobi.trade.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PortfolioResponse {
    private long totalAsset;
    private long cashBalance;
    private long lockedCash;
    private long orderableCash;
    private long stockValuationAmount;
    private long stockPrincipal;
    private long stockProfit;
    private double stockProfitRate;
    private long seedMoney;
    private long totalInvestedPrincipal;
    private long totalProfit;
    private double totalProfitRate;
    private OffsetDateTime quotedAt;
}
