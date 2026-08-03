package org.kkobi.game.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActionLogDto {

    private Long actionLogId;
    private Long userId;
    private Integer gameMonth;
    private String actionType;
    private String assetType;
    private Long actionAmount;
    private String marketState;
    private String depositStatus;
    private Long currentCash;
    private Long currentStock;
    private Long currentDeposit;
    private BigDecimal rtScoreDelta;
    private BigDecimal lhScoreDelta;
    private BigDecimal rpScoreDelta;
}
