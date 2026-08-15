package org.kkobi.assessment.simulation;

import lombok.Getter;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;

import java.math.BigDecimal;

@Getter
public class SimulatedGameAction {

    private final int gameTick;
    private final BehaviorActionType actionType;
    private final BehaviorAssetType assetType;
    private final long actionAmount;
    private final Integer quantity;
    private final Long executionPrice;
    private final BigDecimal positionReturnRate;
    private final BigDecimal realizedReturnRate;
    private final long currentCash;
    private final long currentStockPrincipal;
    private final long currentDeposit;
    private final int currentStockQuantity;

    SimulatedGameAction(
            int gameTick,
            BehaviorActionType actionType,
            BehaviorAssetType assetType,
            long actionAmount,
            Integer quantity,
            Long executionPrice,
            BigDecimal positionReturnRate,
            BigDecimal realizedReturnRate,
            long currentCash,
            long currentStockPrincipal,
            long currentDeposit,
            int currentStockQuantity) {
        this.gameTick = gameTick;
        this.actionType = actionType;
        this.assetType = assetType;
        this.actionAmount = actionAmount;
        this.quantity = quantity;
        this.executionPrice = executionPrice;
        this.positionReturnRate = positionReturnRate;
        this.realizedReturnRate = realizedReturnRate;
        this.currentCash = currentCash;
        this.currentStockPrincipal = currentStockPrincipal;
        this.currentDeposit = currentDeposit;
        this.currentStockQuantity = currentStockQuantity;
    }
}
