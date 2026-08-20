package org.kkobi.assessment.domain;

import lombok.Data;
import org.kkobi.assessment.enums.MarketState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class BehaviorContext {

    private BehaviorEvent currentEvent;
    private List<BehaviorEvent> previousEvents = new ArrayList<>();
    private boolean initialAllocation;
    private boolean fullSecuritySell;
    private boolean sameDayTrade;
    private boolean depositCancelledBeforeSecurityBuy;
    private boolean stockRotation;
    private boolean depositMatured;
    private boolean depositCancelCashRetention;
    private boolean normalPartialSellCashRetention;
    private boolean completedLiquidityOpportunity;
    private boolean riskBudgetMaintenance;
    private BigDecimal stockRatio;
    private BigDecimal cashRatio;
    private BigDecimal depositRatio;
    private BigDecimal averageHoldingDays;
    private BigDecimal sevenDayAverageStockRatio;
    private BigDecimal sevenDayAverageCashRatio;
    private Integer maintainedCashRatioDays;
    private BigDecimal averageDailyTradeCount;
    private MarketState marketState;
    private Integer consecutiveActionCount = 1;
}
