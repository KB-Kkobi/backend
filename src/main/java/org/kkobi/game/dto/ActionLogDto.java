package org.kkobi.game.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class ActionLogDto {

    private Long actionLogId;
    private Long userId;
    private Integer gameTick;
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
    private Timestamp createdAt;
}
