package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.MarketState;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BehaviorRuleEngineTest {

    private final BehaviorRuleEngine behaviorRuleEngine = new BehaviorRuleEngine();

    @Test
    @DisplayName("초기 자산 배분에 해당하는 규칙을 모두 합산한다.")
    void calculateInitialAllocationRules() {
        BehaviorEvent event = new BehaviorEvent();
        event.setActionType(BehaviorActionType.INITIAL_ALLOCATION);
        event.setAssetType(BehaviorAssetType.ALL);

        BehaviorContext context = new BehaviorContext();
        context.setCurrentEvent(event);
        context.setInitialAllocation(true);
        context.setStockRatio(new BigDecimal("70.00"));
        context.setDepositRatio(BigDecimal.ZERO);
        context.setCashRatio(new BigDecimal("30.00"));

        BehaviorAnalysisResult result = behaviorRuleEngine.calculateGameBehaviorAnalysis(context);

        Set<BehaviorRuleCode> appliedRuleCodes = result.getAppliedRules()
                .stream()
                .map(ruleResult -> ruleResult.getRuleCode())
                .collect(Collectors.toSet());
        assertEquals(
                Set.of(
                        BehaviorRuleCode.INITIAL_STOCK_ALLOCATION,
                        BehaviorRuleCode.INITIAL_CASH_ALLOCATION
                ),
                appliedRuleCodes
        );
        assertScoreEquals("5", result.getTotalScoreDelta().getRtDelta());
        assertScoreEquals("5", result.getTotalScoreDelta().getLhDelta());
        assertScoreEquals("0", result.getTotalScoreDelta().getRpDelta());
    }

    @Test
    @DisplayName("한 거래의 중복 규칙 합계에 두 번째 연속 행동 누진율을 적용한다.")
    void calculateStackedRulesWithMultiplier() {
        BehaviorEvent event = new BehaviorEvent();
        event.setActionType(BehaviorActionType.BUY);
        event.setAssetType(BehaviorAssetType.SECURITY);
        event.setPositionReturnRate(new BigDecimal("-20.00"));

        BehaviorContext context = new BehaviorContext();
        context.setCurrentEvent(event);
        context.setMarketState(MarketState.CRASH);
        context.setConsecutiveActionCount(2);

        BehaviorAnalysisResult result = behaviorRuleEngine.calculateGameBehaviorAnalysis(context);

        assertEquals(2, result.getAppliedRules().size());
        assertScoreEquals("30.00", result.getTotalScoreDelta().getRtDelta());
        assertScoreEquals("-12.00", result.getTotalScoreDelta().getLhDelta());
        assertScoreEquals("12.00", result.getTotalScoreDelta().getRpDelta());
    }

    @Test
    @DisplayName("기간 규칙은 여러 주에 해당해도 규칙별로 한 번만 적용한다.")
    void calculateVirtualInvestmentPeriodAnalysisAppliesEachRuleOnce() {
        BehaviorContext firstPeriodContext = createPeriodContext();
        BehaviorContext secondPeriodContext = createPeriodContext();

        BehaviorAnalysisResult result = behaviorRuleEngine.calculateVirtualInvestmentPeriodAnalysis(
                List.of(firstPeriodContext, secondPeriodContext)
        );

        assertEquals(2, result.getAppliedRules().size());
        assertEquals(1, result.getAppliedRules().stream()
                .filter(rule -> rule.getRuleCode() == BehaviorRuleCode.SEVEN_DAY_STOCK_ALLOCATION)
                .count());
        assertEquals(1, result.getAppliedRules().stream()
                .filter(rule -> rule.getRuleCode() == BehaviorRuleCode.LOW_TRADE_FREQUENCY)
                .count());
        assertScoreEquals("10", result.getTotalScoreDelta().getRtDelta());
        assertScoreEquals("-5", result.getTotalScoreDelta().getLhDelta());
        assertScoreEquals("0", result.getTotalScoreDelta().getRpDelta());
    }

    @Test
    @DisplayName("후속 데이터 기반 여섯 규칙을 주기 분석에서 계산한다.")
    void calculateFollowUpPeriodRules() {
        BehaviorContext context = new BehaviorContext();
        context.setDepositCancelledBeforeSecurityBuy(true);
        context.setDepositCancelCashRetention(true);
        context.setNormalPartialSellCashRetention(true);
        context.setCompletedLiquidityOpportunity(true);
        context.setCashRatio(BigDecimal.valueOf(30));
        context.setMaintainedCashRatioDays(5);
        context.setRiskBudgetMaintenance(true);

        BehaviorAnalysisResult result = behaviorRuleEngine
                .calculateVirtualInvestmentPeriodAnalysis(List.of(context));

        assertEquals(6, result.getAppliedRules().size());
        assertScoreEquals("5", result.getTotalScoreDelta().getRtDelta());
        assertScoreEquals("15", result.getTotalScoreDelta().getLhDelta());
        assertScoreEquals("5", result.getTotalScoreDelta().getRpDelta());
    }

    @Test
    @DisplayName("증권 보유 기간 규칙은 가상투자에만 적용한다.")
    void calculateHoldingRulesOnlyForVirtualInvestment() {
        BehaviorEvent gameSellEvent = new BehaviorEvent();
        gameSellEvent.setActionType(BehaviorActionType.SELL);
        gameSellEvent.setAssetType(BehaviorAssetType.SECURITY);
        gameSellEvent.setGameTick(12);

        BehaviorContext gameContext = new BehaviorContext();
        gameContext.setCurrentEvent(gameSellEvent);
        gameContext.setAverageHoldingDays(BigDecimal.valueOf(2));
        BehaviorAnalysisResult gameResult = behaviorRuleEngine
                .calculateGameBehaviorAnalysis(gameContext);

        BehaviorEvent virtualInvestmentSellEvent = new BehaviorEvent();
        virtualInvestmentSellEvent.setActionType(BehaviorActionType.SELL);
        virtualInvestmentSellEvent.setAssetType(BehaviorAssetType.SECURITY);

        BehaviorContext virtualInvestmentShortHoldingContext = new BehaviorContext();
        virtualInvestmentShortHoldingContext.setCurrentEvent(virtualInvestmentSellEvent);
        virtualInvestmentShortHoldingContext.setAverageHoldingDays(BigDecimal.valueOf(2));
        BehaviorAnalysisResult virtualInvestmentShortHoldingResult = behaviorRuleEngine
                .calculateVirtualInvestmentBehaviorAnalysis(virtualInvestmentShortHoldingContext);

        BehaviorContext virtualInvestmentLongHoldingContext = new BehaviorContext();
        virtualInvestmentLongHoldingContext.setAverageHoldingDays(BigDecimal.valueOf(30));
        BehaviorAnalysisResult virtualInvestmentLongHoldingResult = behaviorRuleEngine
                .calculateVirtualInvestmentPeriodAnalysis(List.of(virtualInvestmentLongHoldingContext));

        assertEquals(0, gameResult.getAppliedRules().size());
        assertEquals(BehaviorRuleCode.SHORT_SECURITY_HOLDING,
                virtualInvestmentShortHoldingResult.getAppliedRules().get(0).getRuleCode());
        assertEquals(BehaviorRuleCode.LONG_SECURITY_HOLDING,
                virtualInvestmentLongHoldingResult.getAppliedRules().get(0).getRuleCode());
    }

    @Test
    @DisplayName("게임 분석에서는 가상투자 전용 규칙을 적용하지 않는다.")
    void calculateGameAnalysisExcludesVirtualInvestmentRules() {
        BehaviorEvent gameEvent = new BehaviorEvent();
        gameEvent.setActionType(BehaviorActionType.SELL);
        gameEvent.setAssetType(BehaviorAssetType.SECURITY);
        gameEvent.setGameTick(12);
        gameEvent.setDailyPriceRangeRate(new BigDecimal("10.00"));

        BehaviorContext context = new BehaviorContext();
        context.setCurrentEvent(gameEvent);
        context.setMarketState(MarketState.NORMAL);
        context.setSameDayTrade(true);
        context.setStockRotation(true);
        context.setAverageHoldingDays(BigDecimal.ONE);
        context.setSevenDayAverageStockRatio(new BigDecimal("80.00"));
        context.setMaintainedCashRatioDays(5);
        context.setCashRatio(new BigDecimal("60.00"));
        context.setAverageDailyTradeCount(new BigDecimal("10.00"));

        BehaviorAnalysisResult result = behaviorRuleEngine.calculateGameBehaviorAnalysis(context);

        assertEquals(0, result.getAppliedRules().size());
    }

    @Test
    @DisplayName("가상투자 즉시 분석에서는 게임 초기 배분 규칙을 적용하지 않는다.")
    void calculateVirtualInvestmentAnalysisExcludesGameInitialAllocationRules() {
        BehaviorEvent event = new BehaviorEvent();
        event.setActionType(BehaviorActionType.INITIAL_ALLOCATION);
        event.setAssetType(BehaviorAssetType.ALL);

        BehaviorContext context = new BehaviorContext();
        context.setCurrentEvent(event);
        context.setInitialAllocation(true);
        context.setStockRatio(new BigDecimal("100.00"));

        BehaviorAnalysisResult result = behaviorRuleEngine
                .calculateVirtualInvestmentBehaviorAnalysis(context);

        assertEquals(0, result.getAppliedRules().size());
    }

    private BehaviorContext createPeriodContext() {
        BehaviorContext context = new BehaviorContext();
        context.setStockRatio(new BigDecimal("80.00"));
        context.setCashRatio(new BigDecimal("10.00"));
        context.setSevenDayAverageStockRatio(new BigDecimal("80.00"));
        context.setSevenDayAverageCashRatio(new BigDecimal("10.00"));
        context.setMaintainedCashRatioDays(7);
        context.setAverageDailyTradeCount(BigDecimal.ZERO);
        return context;
    }

    private void assertScoreEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
