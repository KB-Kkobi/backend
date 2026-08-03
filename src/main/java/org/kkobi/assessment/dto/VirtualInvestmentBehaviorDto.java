package org.kkobi.assessment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class VirtualInvestmentBehaviorDto {

    private String actionType;
    private String assetType;
    private Long securityId;
    private String stockCode;
    private Long productOptionId;
    private Integer quantity;
    private Long actionAmount;
    private BigDecimal currentPriceChangeRate;
    private BigDecimal dailyPriceRangeRate;
    private Timestamp tradedAt;
}
