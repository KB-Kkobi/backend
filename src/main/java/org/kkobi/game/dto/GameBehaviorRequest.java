package org.kkobi.game.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GameBehaviorRequest {

    private Long userId;
    private Integer tick;
    private String actionType;
    private String assetType;
    private Long actionAmount;
    private Long currentCash;
    private Long currentStock;
    private Long currentDeposit;
    private BigDecimal changeRate;
    private BigDecimal dailyPriceRangeRate;
    private BigDecimal realizedReturnRate;
    private BigDecimal positionReturnRate;
    private LocalDateTime actedAt;
}
