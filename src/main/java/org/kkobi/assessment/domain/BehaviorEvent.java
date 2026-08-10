package org.kkobi.assessment.domain;

import lombok.Data;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.kkobi.assessment.enums.MarketState;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BehaviorEvent {

    private Long userId;
    private Long accountId;
    private Integer gameTick;
    private Long actionSequence;
    private BehaviorActionType actionType;
    private BehaviorAssetType assetType;
    private Long securityId;
    private String stockCode;
    private Long productOptionId;
    private Long holdingProductId;
    private Integer quantity;
    private Long actionAmount;
    private Long executionPrice;
    private Long currentCash;
    private Long currentStockPrincipal;
    private Long currentDeposit;
    private Integer currentSecurityQuantity;
    private BigDecimal currentPriceChangeRate;
    private BigDecimal dailyPriceRangeRate;
    private BigDecimal realizedReturnRate;
    private BigDecimal positionReturnRate;
    private LocalDateTime tradedAt;
    private MarketState marketState;
}
