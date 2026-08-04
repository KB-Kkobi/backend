package org.kkobi.game.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GameBehaviorRequest {

    private Long userId;
    private String scenarioId;
    private Integer tick;
    private String actionType;
    private String assetType;
    private Long actionAmount;
    private Long currentCash;
    private Long currentStock;
    private Long currentDeposit;
    private BigDecimal dailyPriceRangeRate;
    private BigDecimal realizedReturnRate;
    private BigDecimal positionReturnRate;
}
