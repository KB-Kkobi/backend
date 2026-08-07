package org.kkobi.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class GameActionResponse {

    private final Long actionLogId;
    private final Integer gameTick;
    private final String actionType;
    private final String assetType;
    private final Long actionAmount;
    private final Long currentCash;
    private final Long currentStockPrincipal;
    private final Long currentDeposit;
    private final Long totalAssetPrincipal;
    private final String marketState;
    private final String depositStatus;
    private final BigDecimal rtScoreDelta;
    private final BigDecimal lhScoreDelta;
    private final BigDecimal rpScoreDelta;
}
