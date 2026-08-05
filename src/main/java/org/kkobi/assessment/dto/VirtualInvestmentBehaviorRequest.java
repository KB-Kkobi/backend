package org.kkobi.assessment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VirtualInvestmentBehaviorRequest {

    private Long userId;
    private Long accountId;
    private String actionType;
    private String assetType;
    private Long securityId;
    private String stockCode;
    private Long productOptionId;
    private Integer quantity;
    private Long actionAmount;
    private Long currentCash;
    private Long currentStockPrincipal;
    private Long currentDeposit;
    private BigDecimal currentPriceChangeRate;
    private BigDecimal dailyPriceRangeRate;
    private BigDecimal realizedReturnRate;
    private LocalDateTime tradedAt;
}
