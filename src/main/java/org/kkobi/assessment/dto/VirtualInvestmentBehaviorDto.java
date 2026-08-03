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
    private Long executionPrice;
    private BigDecimal previousClosePrice;
    private BigDecimal currentClosePrice;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Timestamp tradedAt;
}
