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

import static org.junit.jupiter.api.Assertions.assertEquals;

class BehaviorRuleEngineCoverageTest {

    private final BehaviorRuleEngine behaviorRuleEngine = new BehaviorRuleEngine();

    @Test
    @DisplayName("22개 행동 규칙 각각의 RT, LH, RP 변화량을 계산한다.")
    void calculateAllBehaviorRules() {
        List<BehaviorRuleCase> ruleCases = List.of(
                createRuleCase(
                        "초기 주식 비중",
                        createInitialAllocationContext("70", null, null),
                        false,
                        BehaviorRuleCode.INITIAL_STOCK_ALLOCATION,
                        "10", "-5", "5"
                ),
                createRuleCase(
                        "초기 예금 비중",
                        createInitialAllocationContext(null, "50", null),
                        false,
                        BehaviorRuleCode.INITIAL_DEPOSIT_ALLOCATION,
                        "-10", "-5", "-5"
                ),
                createRuleCase(
                        "초기 현금 비중",
                        createInitialAllocationContext(null, null, "30"),
                        false,
                        BehaviorRuleCode.INITIAL_CASH_ALLOCATION,
                        "-5", "10", "-5"
                ),
                createRuleCase(
                        "7일 평균 주식 비중",
                        createMaintainedAllocationContext("70", null),
                        true,
                        BehaviorRuleCode.SEVEN_DAY_STOCK_ALLOCATION,
                        "10", "-5", "5"
                ),
                createRuleCase(
                        "7일 평균 현금 비중",
                        createMaintainedAllocationContext(null, "30"),
                        true,
                        BehaviorRuleCode.SEVEN_DAY_CASH_ALLOCATION,
                        "-5", "10", "-5"
                ),
                createRuleCase(
                        "급락장 매수",
                        createMarketActionContext(MarketState.CRASH, BehaviorActionType.BUY),
                        false,
                        BehaviorRuleCode.CRASH_BUY,
                        "10", "-5", "5"
                ),
                createRuleCase(
                        "급락장 전량 매도",
                        createCrashFullSellContext(),
                        false,
                        BehaviorRuleCode.CRASH_FULL_SELL,
                        "-15", "10", "-5"
                ),
                createRuleCase(
                        "급등장 매수",
                        createMarketActionContext(MarketState.BULL, BehaviorActionType.BUY),
                        false,
                        BehaviorRuleCode.BULL_BUY,
                        "5", "-5", "10"
                ),
                createRuleCase(
                        "급등장 차익 실현",
                        createBullProfitSellContext("0.01"),
                        false,
                        BehaviorRuleCode.BULL_PROFIT_SELL,
                        "0", "5", "5"
                ),
                createRuleCase(
                        "변동성장 당일 매매",
                        createVolatileDayTradeContext("5"),
                        false,
                        BehaviorRuleCode.VOLATILE_DAY_TRADE,
                        "5", "5", "10"
                ),
                createRuleCase(
                        "예금 해지 후 증권 매수",
                        createDepositCancelAndSecurityBuyContext(),
                        false,
                        BehaviorRuleCode.DEPOSIT_CANCEL_AND_SECURITY_BUY,
                        "5", "-10", "10"
                ),
                createRuleCase(
                        "예금 만기 유지",
                        createDepositMaturityContext(),
                        false,
                        BehaviorRuleCode.DEPOSIT_MATURITY,
                        "-5", "-10", "-5"
                ),
                createRuleCase(
                        "3일 미만 증권 보유",
                        createShortHoldingContext("2.99"),
                        false,
                        BehaviorRuleCode.SHORT_SECURITY_HOLDING,
                        "5", "5", "10"
                ),
                createRuleCase(
                        "30일 이상 증권 보유",
                        createLongHoldingContext("30"),
                        true,
                        BehaviorRuleCode.LONG_SECURITY_HOLDING,
                        "0", "-5", "-5"
                ),
                createRuleCase(
                        "손실 종목 추가 매수",
                        createLossAveragingContext("-15"),
                        false,
                        BehaviorRuleCode.LOSS_AVERAGING_BUY,
                        "15", "-5", "5"
                ),
                createRuleCase(
                        "손절매",
                        createLossCutContext("-10"),
                        false,
                        BehaviorRuleCode.LOSS_CUT_SELL,
                        "-5", "5", "-5"
                ),
                createRuleCase(
                        "종목 교체",
                        createStockRotationContext(),
                        false,
                        BehaviorRuleCode.STOCK_ROTATION,
                        "5", "-5", "10"
                ),
                createRuleCase(
                        "현금 비중 5% 미만 유지",
                        createCashMaintenanceContext("4.99", 5),
                        true,
                        BehaviorRuleCode.VERY_LOW_CASH_MAINTENANCE,
                        "15", "-15", "5"
                ),
                createRuleCase(
                        "현금 비중 25% 이상 50% 미만 유지",
                        createCashMaintenanceContext("25", 5),
                        true,
                        BehaviorRuleCode.MEDIUM_CASH_MAINTENANCE,
                        "0", "5", "0"
                ),
                createRuleCase(
                        "현금 비중 50% 이상 유지",
                        createCashMaintenanceContext("50", 5),
                        true,
                        BehaviorRuleCode.HIGH_CASH_MAINTENANCE,
                        "-10", "15", "-5"
                ),
                createRuleCase(
                        "높은 거래 빈도",
                        createTradeFrequencyContext("5"),
                        true,
                        BehaviorRuleCode.HIGH_TRADE_FREQUENCY,
                        "0", "0", "5"
                ),
                createRuleCase(
                        "낮은 거래 빈도",
                        createTradeFrequencyContext("0.2"),
                        true,
                        BehaviorRuleCode.LOW_TRADE_FREQUENCY,
                        "0", "0", "-5"
                )
        );

        assertEquals(22, ruleCases.size());
        ruleCases.forEach(this::assertBehaviorRule);
    }

    @Test
    @DisplayName("초기 배분과 7일 평균 비중 임계값의 직전과 경계를 구분한다.")
    void calculateAllocationThresholdBoundaries() {
        assertRulePresence(
                createInitialAllocationContext("69.99", null, null),
                false,
                BehaviorRuleCode.INITIAL_STOCK_ALLOCATION,
                false
        );
        assertRulePresence(
                createInitialAllocationContext("70", null, null),
                false,
                BehaviorRuleCode.INITIAL_STOCK_ALLOCATION,
                true
        );
        assertRulePresence(
                createInitialAllocationContext(null, "49.99", null),
                false,
                BehaviorRuleCode.INITIAL_DEPOSIT_ALLOCATION,
                false
        );
        assertRulePresence(
                createInitialAllocationContext(null, "50", null),
                false,
                BehaviorRuleCode.INITIAL_DEPOSIT_ALLOCATION,
                true
        );
        assertRulePresence(
                createInitialAllocationContext(null, null, "29.99"),
                false,
                BehaviorRuleCode.INITIAL_CASH_ALLOCATION,
                false
        );
        assertRulePresence(
                createInitialAllocationContext(null, null, "30"),
                false,
                BehaviorRuleCode.INITIAL_CASH_ALLOCATION,
                true
        );
        assertRulePresence(
                createMaintainedAllocationContext("69.99", null),
                true,
                BehaviorRuleCode.SEVEN_DAY_STOCK_ALLOCATION,
                false
        );
        assertRulePresence(
                createMaintainedAllocationContext("70", null),
                true,
                BehaviorRuleCode.SEVEN_DAY_STOCK_ALLOCATION,
                true
        );
        assertRulePresence(
                createMaintainedAllocationContext(null, "29.99"),
                true,
                BehaviorRuleCode.SEVEN_DAY_CASH_ALLOCATION,
                false
        );
        assertRulePresence(
                createMaintainedAllocationContext(null, "30"),
                true,
                BehaviorRuleCode.SEVEN_DAY_CASH_ALLOCATION,
                true
        );
    }

    @Test
    @DisplayName("시장 행동과 손익 대응 임계값의 직전과 경계를 구분한다.")
    void calculateMarketAndReturnThresholdBoundaries() {
        assertRulePresence(
                createVolatileDayTradeContext("4.99"),
                false,
                BehaviorRuleCode.VOLATILE_DAY_TRADE,
                false
        );
        assertRulePresence(
                createVolatileDayTradeContext("5"),
                false,
                BehaviorRuleCode.VOLATILE_DAY_TRADE,
                true
        );
        assertRulePresence(
                createBullProfitSellContext("0"),
                false,
                BehaviorRuleCode.BULL_PROFIT_SELL,
                false
        );
        assertRulePresence(
                createBullProfitSellContext("0.01"),
                false,
                BehaviorRuleCode.BULL_PROFIT_SELL,
                true
        );
        assertRulePresence(
                createLossAveragingContext("-14.99"),
                false,
                BehaviorRuleCode.LOSS_AVERAGING_BUY,
                false
        );
        assertRulePresence(
                createLossAveragingContext("-15"),
                false,
                BehaviorRuleCode.LOSS_AVERAGING_BUY,
                true
        );
        assertRulePresence(
                createLossCutContext("-9.99"),
                false,
                BehaviorRuleCode.LOSS_CUT_SELL,
                false
        );
        assertRulePresence(
                createLossCutContext("-10"),
                false,
                BehaviorRuleCode.LOSS_CUT_SELL,
                true
        );
    }

    @Test
    @DisplayName("보유 기간, 현금 유지 기간과 거래 빈도 임계값을 구분한다.")
    void calculatePeriodThresholdBoundaries() {
        assertRulePresence(
                createShortHoldingContext("2.99"),
                false,
                BehaviorRuleCode.SHORT_SECURITY_HOLDING,
                true
        );
        assertRulePresence(
                createShortHoldingContext("3"),
                false,
                BehaviorRuleCode.SHORT_SECURITY_HOLDING,
                false
        );
        assertRulePresence(
                createLongHoldingContext("29.99"),
                true,
                BehaviorRuleCode.LONG_SECURITY_HOLDING,
                false
        );
        assertRulePresence(
                createLongHoldingContext("30"),
                true,
                BehaviorRuleCode.LONG_SECURITY_HOLDING,
                true
        );
        assertRulePresence(
                createCashMaintenanceContext("25", 4),
                true,
                BehaviorRuleCode.MEDIUM_CASH_MAINTENANCE,
                false
        );
        assertRulePresence(
                createCashMaintenanceContext("4.99", 5),
                true,
                BehaviorRuleCode.VERY_LOW_CASH_MAINTENANCE,
                true
        );
        assertNoCashMaintenanceRule(createCashMaintenanceContext("5", 5));
        assertNoCashMaintenanceRule(createCashMaintenanceContext("24.99", 5));
        assertRulePresence(
                createCashMaintenanceContext("25", 5),
                true,
                BehaviorRuleCode.MEDIUM_CASH_MAINTENANCE,
                true
        );
        assertRulePresence(
                createCashMaintenanceContext("49.99", 5),
                true,
                BehaviorRuleCode.MEDIUM_CASH_MAINTENANCE,
                true
        );
        assertRulePresence(
                createCashMaintenanceContext("50", 5),
                true,
                BehaviorRuleCode.HIGH_CASH_MAINTENANCE,
                true
        );
        assertRulePresence(
                createTradeFrequencyContext("4.99"),
                true,
                BehaviorRuleCode.HIGH_TRADE_FREQUENCY,
                false
        );
        assertRulePresence(
                createTradeFrequencyContext("5"),
                true,
                BehaviorRuleCode.HIGH_TRADE_FREQUENCY,
                true
        );
        assertRulePresence(
                createTradeFrequencyContext("0.21"),
                true,
                BehaviorRuleCode.LOW_TRADE_FREQUENCY,
                false
        );
        assertRulePresence(
                createTradeFrequencyContext("0.2"),
                true,
                BehaviorRuleCode.LOW_TRADE_FREQUENCY,
                true
        );
    }

    @Test
    @DisplayName("동일 행동의 1회, 2회, 3회 누진율을 적용한다.")
    void calculateConsecutiveActionMultiplierBoundaries() {
        assertScoreEquals(
                "10",
                calculateConsecutiveCrashBuy(1).getTotalScoreDelta().getRtDelta(),
                "1회차"
        );
        assertScoreEquals(
                "12",
                calculateConsecutiveCrashBuy(2).getTotalScoreDelta().getRtDelta(),
                "2회차"
        );
        assertScoreEquals(
                "15",
                calculateConsecutiveCrashBuy(3).getTotalScoreDelta().getRtDelta(),
                "3회차"
        );
    }

    private BehaviorRuleCase createRuleCase(
            String name,
            BehaviorContext context,
            boolean periodRule,
            BehaviorRuleCode ruleCode,
            String rtDelta,
            String lhDelta,
            String rpDelta) {
        return new BehaviorRuleCase(
                name,
                context,
                periodRule,
                ruleCode,
                rtDelta,
                lhDelta,
                rpDelta
        );
    }

    private void assertBehaviorRule(BehaviorRuleCase ruleCase) {
        BehaviorAnalysisResult result = calculateBehaviorAnalysis(
                ruleCase.context(),
                ruleCase.periodRule()
        );

        assertEquals(1, result.getAppliedRules().size(), ruleCase.name());
        assertEquals(
                ruleCase.ruleCode(),
                result.getAppliedRules().get(0).getRuleCode(),
                ruleCase.name()
        );
        assertScoreEquals(
                ruleCase.rtDelta(),
                result.getTotalScoreDelta().getRtDelta(),
                ruleCase.name() + " RT"
        );
        assertScoreEquals(
                ruleCase.lhDelta(),
                result.getTotalScoreDelta().getLhDelta(),
                ruleCase.name() + " LH"
        );
        assertScoreEquals(
                ruleCase.rpDelta(),
                result.getTotalScoreDelta().getRpDelta(),
                ruleCase.name() + " RP"
        );
    }

    private void assertRulePresence(
            BehaviorContext context,
            boolean periodRule,
            BehaviorRuleCode ruleCode,
            boolean expected) {
        BehaviorAnalysisResult result = calculateBehaviorAnalysis(context, periodRule);
        boolean applied = result.getAppliedRules().stream()
                .anyMatch(rule -> rule.getRuleCode() == ruleCode);
        assertEquals(expected, applied, ruleCode.name());
    }

    private void assertNoCashMaintenanceRule(BehaviorContext context) {
        BehaviorAnalysisResult result = calculateBehaviorAnalysis(context, true);
        boolean applied = result.getAppliedRules().stream()
                .map(rule -> rule.getRuleCode())
                .anyMatch(ruleCode -> ruleCode == BehaviorRuleCode.VERY_LOW_CASH_MAINTENANCE
                        || ruleCode == BehaviorRuleCode.MEDIUM_CASH_MAINTENANCE
                        || ruleCode == BehaviorRuleCode.HIGH_CASH_MAINTENANCE);
        assertEquals(false, applied);
    }

    private BehaviorAnalysisResult calculateBehaviorAnalysis(
            BehaviorContext context,
            boolean periodRule) {
        if (periodRule) {
            return behaviorRuleEngine.calculateVirtualInvestmentPeriodAnalysis(List.of(context));
        }
        if (context.isInitialAllocation()) {
            return behaviorRuleEngine.calculateGameBehaviorAnalysis(context);
        }
        return behaviorRuleEngine.calculateVirtualInvestmentBehaviorAnalysis(context);
    }

    private BehaviorAnalysisResult calculateConsecutiveCrashBuy(int consecutiveActionCount) {
        BehaviorContext context = createMarketActionContext(
                MarketState.CRASH,
                BehaviorActionType.BUY
        );
        context.setConsecutiveActionCount(consecutiveActionCount);
        return behaviorRuleEngine.calculateGameBehaviorAnalysis(context);
    }

    private BehaviorContext createInitialAllocationContext(
            String stockRatio,
            String depositRatio,
            String cashRatio) {
        BehaviorEvent event = createEvent(
                BehaviorActionType.INITIAL_ALLOCATION,
                BehaviorAssetType.ALL
        );
        event.setGameTick(0);

        BehaviorContext context = new BehaviorContext();
        context.setCurrentEvent(event);
        context.setInitialAllocation(true);
        context.setStockRatio(createBigDecimal(stockRatio));
        context.setDepositRatio(createBigDecimal(depositRatio));
        context.setCashRatio(createBigDecimal(cashRatio));
        return context;
    }

    private BehaviorContext createMaintainedAllocationContext(
            String stockRatio,
            String cashRatio) {
        BehaviorContext context = new BehaviorContext();
        context.setSevenDayAverageStockRatio(createBigDecimal(stockRatio));
        context.setSevenDayAverageCashRatio(createBigDecimal(cashRatio));
        return context;
    }

    private BehaviorContext createMarketActionContext(
            MarketState marketState,
            BehaviorActionType actionType) {
        BehaviorContext context = createSecurityActionContext(actionType);
        context.setMarketState(marketState);
        return context;
    }

    private BehaviorContext createCrashFullSellContext() {
        BehaviorContext context = createMarketActionContext(
                MarketState.CRASH,
                BehaviorActionType.SELL
        );
        context.setFullSecuritySell(true);
        return context;
    }

    private BehaviorContext createBullProfitSellContext(String realizedReturnRate) {
        BehaviorContext context = createMarketActionContext(
                MarketState.BULL,
                BehaviorActionType.SELL
        );
        context.getCurrentEvent().setRealizedReturnRate(
                createBigDecimal(realizedReturnRate)
        );
        return context;
    }

    private BehaviorContext createVolatileDayTradeContext(String dailyPriceRangeRate) {
        BehaviorContext context = createMarketActionContext(
                MarketState.NORMAL,
                BehaviorActionType.SELL
        );
        context.setSameDayTrade(true);
        context.getCurrentEvent().setDailyPriceRangeRate(
                createBigDecimal(dailyPriceRangeRate)
        );
        return context;
    }

    private BehaviorContext createDepositCancelAndSecurityBuyContext() {
        BehaviorContext context = createSecurityActionContext(BehaviorActionType.BUY);
        context.setMarketState(MarketState.NORMAL);
        context.setDepositCancelledBeforeSecurityBuy(true);
        return context;
    }

    private BehaviorContext createDepositMaturityContext() {
        BehaviorContext context = new BehaviorContext();
        context.setDepositMatured(true);
        return context;
    }

    private BehaviorContext createShortHoldingContext(String holdingDays) {
        BehaviorContext context = createSecurityActionContext(BehaviorActionType.SELL);
        context.setAverageHoldingDays(createBigDecimal(holdingDays));
        return context;
    }

    private BehaviorContext createLongHoldingContext(String holdingDays) {
        BehaviorContext context = new BehaviorContext();
        context.setAverageHoldingDays(createBigDecimal(holdingDays));
        return context;
    }

    private BehaviorContext createLossAveragingContext(String positionReturnRate) {
        BehaviorContext context = createSecurityActionContext(BehaviorActionType.BUY);
        context.setMarketState(MarketState.NORMAL);
        context.getCurrentEvent().setPositionReturnRate(
                createBigDecimal(positionReturnRate)
        );
        return context;
    }

    private BehaviorContext createLossCutContext(String realizedReturnRate) {
        BehaviorContext context = createSecurityActionContext(BehaviorActionType.SELL);
        context.setMarketState(MarketState.NORMAL);
        context.getCurrentEvent().setRealizedReturnRate(
                createBigDecimal(realizedReturnRate)
        );
        return context;
    }

    private BehaviorContext createStockRotationContext() {
        BehaviorContext context = new BehaviorContext();
        context.setStockRotation(true);
        return context;
    }

    private BehaviorContext createCashMaintenanceContext(
            String cashRatio,
            int maintainedDays) {
        BehaviorContext context = new BehaviorContext();
        context.setCashRatio(createBigDecimal(cashRatio));
        context.setMaintainedCashRatioDays(maintainedDays);
        return context;
    }

    private BehaviorContext createTradeFrequencyContext(String averageDailyTradeCount) {
        BehaviorContext context = new BehaviorContext();
        context.setAverageDailyTradeCount(createBigDecimal(averageDailyTradeCount));
        return context;
    }

    private BehaviorContext createSecurityActionContext(BehaviorActionType actionType) {
        BehaviorContext context = new BehaviorContext();
        context.setCurrentEvent(createEvent(actionType, BehaviorAssetType.SECURITY));
        return context;
    }

    private BehaviorEvent createEvent(
            BehaviorActionType actionType,
            BehaviorAssetType assetType) {
        BehaviorEvent event = new BehaviorEvent();
        event.setActionType(actionType);
        event.setAssetType(assetType);
        return event;
    }

    private BigDecimal createBigDecimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private void assertScoreEquals(
            String expected,
            BigDecimal actual,
            String message) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), message);
    }

    private record BehaviorRuleCase(
            String name,
            BehaviorContext context,
            boolean periodRule,
            BehaviorRuleCode ruleCode,
            String rtDelta,
            String lhDelta,
            String rpDelta) {
    }
}
