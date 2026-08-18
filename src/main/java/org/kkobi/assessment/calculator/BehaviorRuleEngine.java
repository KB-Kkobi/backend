package org.kkobi.assessment.calculator;

import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.domain.RuleResult;
import org.kkobi.assessment.domain.ScoreDelta;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.MarketState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BehaviorRuleEngine {

    private static final BigDecimal STOCK_ALLOCATION_THRESHOLD = BigDecimal.valueOf(70);
    private static final BigDecimal DEPOSIT_ALLOCATION_THRESHOLD = BigDecimal.valueOf(50);
    private static final BigDecimal CASH_ALLOCATION_THRESHOLD = BigDecimal.valueOf(30);
    private static final BigDecimal VOLATILE_RATE_THRESHOLD = BigDecimal.valueOf(5);
    private static final BigDecimal LOSS_AVERAGING_RATE = BigDecimal.valueOf(-15);
    private static final BigDecimal LOSS_CUT_RATE = BigDecimal.valueOf(-10);
    private static final BigDecimal TEN = BigDecimal.valueOf(10);
    private static final BigDecimal TWENTY = BigDecimal.valueOf(20);
    private static final BigDecimal THIRTY = BigDecimal.valueOf(30);
    private static final BigDecimal FIFTY = BigDecimal.valueOf(50);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal SHORT_HOLDING_DAYS = BigDecimal.valueOf(3);
    private static final BigDecimal LONG_HOLDING_DAYS = BigDecimal.valueOf(30);
    private static final BigDecimal VERY_LOW_CASH_RATIO = BigDecimal.valueOf(5);
    private static final BigDecimal MEDIUM_CASH_RATIO = BigDecimal.valueOf(25);
    private static final BigDecimal HIGH_CASH_RATIO = BigDecimal.valueOf(50);
    private static final BigDecimal HIGH_TRADE_FREQUENCY = BigDecimal.valueOf(5);
    private static final BigDecimal LOW_TRADE_FREQUENCY = BigDecimal.valueOf(0.2);
    private static final int CASH_MAINTENANCE_DAYS = 5;

    public BehaviorAnalysisResult calculateGameBehaviorAnalysis(BehaviorContext behaviorContext) {
        List<RuleResult> appliedRules = new ArrayList<>();

        calculateInitialAllocationRules(behaviorContext, appliedRules);
        calculateMarketActionRules(behaviorContext, appliedRules, false);
        calculateDepositRules(behaviorContext, appliedRules);
        calculateReturnResponseRules(behaviorContext, appliedRules);

        return new BehaviorAnalysisResult(applyConsecutiveActionMultiplier(behaviorContext, appliedRules));
    }

    public BehaviorAnalysisResult calculateVirtualInvestmentBehaviorAnalysis(
            BehaviorContext behaviorContext) {
        List<RuleResult> appliedRules = new ArrayList<>();

        calculateVirtualInvestmentMarketActionRules(behaviorContext, appliedRules);
        calculateDepositMaturityRule(behaviorContext, appliedRules);
        calculateHoldingPeriodRules(behaviorContext, appliedRules);
        calculateStockRotationRule(behaviorContext, appliedRules);

        return new BehaviorAnalysisResult(applyConsecutiveActionMultiplier(behaviorContext, appliedRules));
    }

    public BehaviorAnalysisResult calculateVirtualInvestmentPeriodAnalysis(
            List<BehaviorContext> behaviorContexts) {
        Map<BehaviorRuleCode, RuleResult> appliedRuleByCode = new LinkedHashMap<>();

        for (BehaviorContext behaviorContext : behaviorContexts) {
            List<RuleResult> periodRules = new ArrayList<>();
            calculateMaintainedAllocationRules(behaviorContext, periodRules);
            calculateMaintainedCashRules(behaviorContext, periodRules);
            calculateTradeFrequencyRules(behaviorContext, periodRules);
            calculateLongHoldingPeriodRule(behaviorContext, periodRules);
            calculateFollowUpBehaviorRules(behaviorContext, periodRules);
            periodRules.forEach(ruleResult -> appliedRuleByCode.putIfAbsent(
                    ruleResult.getRuleCode(),
                    ruleResult
            ));
        }

        return new BehaviorAnalysisResult(new ArrayList<>(appliedRuleByCode.values()));
    }

    private void calculateInitialAllocationRules(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        if (!context.isInitialAllocation()) {
            return;
        }

        if (isGreaterThanOrEqual(context.getStockRatio(), STOCK_ALLOCATION_THRESHOLD)) {
            addRule(appliedRules, BehaviorRuleCode.INITIAL_STOCK_ALLOCATION, 10, -5, 5,
                    "초기 주식 비중이 70% 이상입니다.");
        }
        if (isGreaterThanOrEqual(context.getDepositRatio(), DEPOSIT_ALLOCATION_THRESHOLD)) {
            addRule(appliedRules, BehaviorRuleCode.INITIAL_DEPOSIT_ALLOCATION, -10, -5, -5,
                    "초기 예금 비중이 50% 이상입니다.");
        }
        if (isGreaterThanOrEqual(context.getCashRatio(), CASH_ALLOCATION_THRESHOLD)) {
            addRule(appliedRules, BehaviorRuleCode.INITIAL_CASH_ALLOCATION, -5, 10, -5,
                    "초기 현금 비중이 30% 이상입니다.");
        }
    }

    private void calculateMaintainedAllocationRules(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        if (isGreaterThanOrEqual(context.getSevenDayAverageStockRatio(), STOCK_ALLOCATION_THRESHOLD)) {
            addRule(appliedRules, BehaviorRuleCode.SEVEN_DAY_STOCK_ALLOCATION, 10, -5, 5,
                    "첫 매매 후 7일 평균 주식 비중이 70% 이상입니다.");
        }
        if (isGreaterThanOrEqual(context.getSevenDayAverageCashRatio(), CASH_ALLOCATION_THRESHOLD)) {
            addRule(appliedRules, BehaviorRuleCode.SEVEN_DAY_CASH_ALLOCATION, -5, 10, -5,
                    "첫 매매 후 7일 평균 현금 비중이 30% 이상입니다.");
        }
    }

    private void calculateMarketActionRules(
            BehaviorContext context,
            List<RuleResult> appliedRules,
            boolean includeVolatileDayTrade) {
        BehaviorEvent event = context.getCurrentEvent();
        if (event == null || event.getAssetType() != BehaviorAssetType.SECURITY) {
            return;
        }

        if (context.getMarketState() == MarketState.CRASH && event.getActionType() == BehaviorActionType.BUY) {
            addRule(appliedRules, BehaviorRuleCode.CRASH_BUY, 10, -5, 5,
                    "급락 상황에서 증권을 추가 매수했습니다.");
        }
        if (context.getMarketState() == MarketState.CRASH
                && event.getActionType() == BehaviorActionType.SELL
                && context.isFullSecuritySell()) {
            addRule(appliedRules, BehaviorRuleCode.CRASH_FULL_SELL, -15, 10, -5,
                    "급락 상황에서 보유 증권을 전량 매도했습니다.");
        }
        if (context.getMarketState() == MarketState.BULL && event.getActionType() == BehaviorActionType.BUY) {
            addRule(appliedRules, BehaviorRuleCode.BULL_BUY, 5, -5, 10,
                    "급등 상황에서 추세 매수했습니다.");
        }
        if (context.getMarketState() == MarketState.BULL
                && event.getActionType() == BehaviorActionType.SELL
                && isGreaterThan(event.getRealizedReturnRate(), BigDecimal.ZERO)) {
            addRule(appliedRules, BehaviorRuleCode.BULL_PROFIT_SELL, 0, 5, 5,
                    "급등 상황에서 수익을 실현했습니다.");
        }
        if (includeVolatileDayTrade
                && context.isSameDayTrade()
                && isGreaterThanOrEqual(event.getDailyPriceRangeRate(), VOLATILE_RATE_THRESHOLD)) {
            addRule(appliedRules, BehaviorRuleCode.VOLATILE_DAY_TRADE, 5, 5, 10,
                    "변동폭 5% 이상인 날에 당일 매매했습니다.");
        }
    }

    private void calculateVirtualInvestmentMarketActionRules(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        BehaviorEvent event = context.getCurrentEvent();
        if (event == null || event.getAssetType() != BehaviorAssetType.SECURITY) {
            return;
        }

        if (event.getActionType() == BehaviorActionType.BUY) {
            calculateVirtualInvestmentBuyRule(context, appliedRules);
        } else if (event.getActionType() == BehaviorActionType.SELL) {
            calculateVirtualInvestmentSellRule(context, appliedRules);
        }
    }

    private void calculateVirtualInvestmentBuyRule(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        BehaviorEvent event = context.getCurrentEvent();
        BigDecimal buyRatio = calculateActionAmountRatio(event);
        if (buyRatio.compareTo(TEN) < 0) {
            return;
        }

        if (isLessThanOrEqual(event.getPositionReturnRate(), LOSS_AVERAGING_RATE)) {
            if (buyRatio.compareTo(THIRTY) >= 0) {
                addRule(appliedRules, BehaviorRuleCode.LOSS_AVERAGING_BUY, 15, -10, 0,
                        "손실률 -15% 이하에서 자산의 30% 이상을 추가 매수했습니다.");
            } else {
                addRule(appliedRules, BehaviorRuleCode.LOSS_AVERAGING_BUY, 10, -5, 0,
                        "손실률 -15% 이하에서 자산의 10~30%를 추가 매수했습니다.");
            }
            return;
        }

        if (context.getMarketState() == MarketState.CRASH) {
            if (buyRatio.compareTo(THIRTY) >= 0) {
                addRule(appliedRules, BehaviorRuleCode.CRASH_BUY, 15, -10, 0,
                        "급락장에서 자산의 30% 이상을 매수했습니다.");
            } else {
                addRule(appliedRules, BehaviorRuleCode.CRASH_BUY, 10, -5, 0,
                        "급락장에서 자산의 10~30%를 매수했습니다.");
            }
            return;
        }

        if (context.getMarketState() == MarketState.BULL) {
            if (buyRatio.compareTo(THIRTY) >= 0) {
                addRule(appliedRules, BehaviorRuleCode.BULL_BUY, 10, -10, 5,
                        "급등장에서 자산의 30% 이상을 추세 매수했습니다.");
            } else {
                addRule(appliedRules, BehaviorRuleCode.BULL_BUY, 0, -5, 10,
                        "급등장에서 자산의 10~30%를 추세 매수했습니다.");
            }
            return;
        }

        if (context.getMarketState() == MarketState.NORMAL
                && buyRatio.compareTo(THIRTY) < 0) {
            addRule(appliedRules, BehaviorRuleCode.NORMAL_PLANNED_BUY, 0, -5, 5,
                    "정상장에서 자산의 10~30%를 계획 매수했습니다.");
        }
    }

    private void calculateVirtualInvestmentSellRule(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        BehaviorEvent event = context.getCurrentEvent();
        BigDecimal sellRatio = calculateSecuritySellRatio(event);
        if (sellRatio.compareTo(TWENTY) < 0) {
            return;
        }

        if (context.getMarketState() == MarketState.CRASH) {
            if (context.isFullSecuritySell()) {
                addRule(appliedRules, BehaviorRuleCode.CRASH_FULL_SELL, -15, 10, -5,
                        "급락장에서 보유 증권을 전량 매도했습니다.");
            } else if (sellRatio.compareTo(FIFTY) >= 0) {
                addRule(appliedRules, BehaviorRuleCode.CRASH_FULL_SELL, -10, 5, -5,
                        "급락장에서 보유 증권의 50% 이상을 매도했습니다.");
            } else {
                addRule(appliedRules, BehaviorRuleCode.CRASH_FULL_SELL, 0, 5, -5,
                        "급락장에서 보유 증권의 20~50%를 매도했습니다.");
            }
            return;
        }

        if (isLessThanOrEqual(event.getRealizedReturnRate(), LOSS_CUT_RATE)) {
            if (context.isFullSecuritySell()) {
                addRule(appliedRules, BehaviorRuleCode.LOSS_CUT_SELL, -15, 10, -10,
                        "손실률 -10% 이하에서 보유 증권을 전량 손절했습니다.");
            } else if (sellRatio.compareTo(FIFTY) >= 0) {
                addRule(appliedRules, BehaviorRuleCode.LOSS_CUT_SELL, -10, 10, -5,
                        "손실률 -10% 이하에서 보유 증권의 50% 이상을 손절했습니다.");
            } else {
                addRule(appliedRules, BehaviorRuleCode.LOSS_CUT_SELL, -5, 5, -5,
                        "손실률 -10% 이하에서 보유 증권의 20~50%를 손절했습니다.");
            }
            return;
        }

        if (context.getMarketState() == MarketState.BULL
                && isGreaterThan(event.getRealizedReturnRate(), BigDecimal.ZERO)) {
            if (sellRatio.compareTo(FIFTY) >= 0) {
                addRule(appliedRules, BehaviorRuleCode.BULL_PROFIT_SELL, 0, 10, 0,
                        "급등장에서 보유 증권의 50% 이상을 수익 실현했습니다.");
            } else {
                addRule(appliedRules, BehaviorRuleCode.BULL_PROFIT_SELL, 0, 5, 5,
                        "급등장에서 보유 증권의 20~50%를 수익 실현했습니다.");
            }
        }

        if (context.isSameDayTrade()
                && isGreaterThanOrEqual(event.getDailyPriceRangeRate(), VOLATILE_RATE_THRESHOLD)) {
            addRule(appliedRules, BehaviorRuleCode.VOLATILE_DAY_TRADE, 5, 5, 10,
                    "변동폭 5% 이상인 날에 당일 매매했습니다.");
        }
    }

    private BigDecimal calculateActionAmountRatio(BehaviorEvent event) {
        if (event.getActionAmount() == null
                || event.getCurrentCash() == null
                || event.getCurrentStockPrincipal() == null
                || event.getCurrentDeposit() == null) {
            return BigDecimal.ZERO;
        }
        long totalAssets = Math.addExact(
                Math.addExact(event.getCurrentCash(), event.getCurrentStockPrincipal()),
                event.getCurrentDeposit()
        );
        return calculateRatio(event.getActionAmount(), totalAssets);
    }

    private BigDecimal calculateSecuritySellRatio(BehaviorEvent event) {
        if (event.getQuantity() == null || event.getCurrentSecurityQuantity() == null) {
            return BigDecimal.ZERO;
        }
        long quantityBeforeSell = (long) event.getCurrentSecurityQuantity() + event.getQuantity();
        return calculateRatio(event.getQuantity(), quantityBeforeSell);
    }

    private BigDecimal calculateRatio(long amount, long total) {
        if (amount <= 0L || total <= 0L) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amount)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    private void calculateDepositRules(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        BehaviorEvent event = context.getCurrentEvent();
        if (event != null
                && event.getActionType() == BehaviorActionType.BUY
                && event.getAssetType() == BehaviorAssetType.SECURITY
                && context.isDepositCancelledBeforeSecurityBuy()) {
            addRule(appliedRules, BehaviorRuleCode.DEPOSIT_CANCEL_AND_SECURITY_BUY, 5, -10, 10,
                    "예금을 중도 해지한 후 증권을 매수했습니다.");
        }
        if (context.isDepositMatured()) {
            addRule(appliedRules, BehaviorRuleCode.DEPOSIT_MATURITY, -5, -10, -5,
                    "예금을 만기까지 유지했습니다.");
        }
    }

    private void calculateDepositMaturityRule(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        if (context.isDepositMatured()) {
            addRule(appliedRules, BehaviorRuleCode.DEPOSIT_MATURITY, -5, -10, -5,
                    "예금을 만기까지 유지했습니다.");
        }
    }

    private void calculateFollowUpBehaviorRules(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        if (context.isDepositCancelledBeforeSecurityBuy()) {
            addRule(appliedRules, BehaviorRuleCode.DEPOSIT_CANCEL_AND_SECURITY_BUY, 5, -10, 10,
                    "예금 해지 후 2일 이내에 증권을 매수했습니다.");
        }
        if (context.isDepositCancelCashRetention()) {
            addRule(appliedRules, BehaviorRuleCode.DEPOSIT_CANCEL_CASH_RETENTION, -5, 10, -5,
                    "예금 해지 후 2일 동안 현금의 80% 이상을 유지했습니다.");
        }
        if (context.isNormalPartialSellCashRetention()) {
            addRule(appliedRules, BehaviorRuleCode.NORMAL_PARTIAL_SELL, 0, 5, 5,
                    "정상장 부분 매도 후 현금 비중 25~50%를 2일 유지했습니다.");
        }
        if (context.isCompletedLiquidityOpportunity()) {
            addRule(appliedRules, BehaviorRuleCode.LHH_COMPLETED_OPPORTUNITY, 0, 0, 5,
                    "유동성을 보존하며 매수 후 10일 이내 수익 매도를 완료했습니다.");
        }
        if (context.isRiskBudgetMaintenance()) {
            addRule(appliedRules, BehaviorRuleCode.RISK_BUDGET_MAINTENANCE, 5, 5, -5,
                    "주식 50~70%와 현금 25~40%를 무거래 5일 유지했습니다.");
        }
    }

    private void calculateHoldingPeriodRules(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        if (context.getCurrentEvent() == null
                || context.getCurrentEvent().getGameTick() != null) {
            return;
        }

        if (isLessThan(context.getAverageHoldingDays(), SHORT_HOLDING_DAYS)) {
            addRule(appliedRules, BehaviorRuleCode.SHORT_SECURITY_HOLDING, 5, 5, 10,
                    "증권 평균 보유 기간이 3일 미만입니다.");
        }
    }

    private void calculateLongHoldingPeriodRule(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        if (isGreaterThanOrEqual(context.getAverageHoldingDays(), LONG_HOLDING_DAYS)) {
            addRule(appliedRules, BehaviorRuleCode.LONG_SECURITY_HOLDING, 0, -5, -5,
                    "증권 평균 보유 기간이 30일 이상입니다.");
        }
    }

    private void calculateReturnResponseRules(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        BehaviorEvent event = context.getCurrentEvent();
        if (event == null || event.getAssetType() != BehaviorAssetType.SECURITY) {
            return;
        }

        if (event.getActionType() == BehaviorActionType.BUY
                && isLessThanOrEqual(event.getPositionReturnRate(), LOSS_AVERAGING_RATE)) {
            addRule(appliedRules, BehaviorRuleCode.LOSS_AVERAGING_BUY, 15, -5, 5,
                    "손실률 -15% 이하인 종목을 추가 매수했습니다.");
        }
        if (event.getActionType() == BehaviorActionType.SELL
                && isLessThanOrEqual(event.getRealizedReturnRate(), LOSS_CUT_RATE)) {
            addRule(appliedRules, BehaviorRuleCode.LOSS_CUT_SELL, -5, 5, -5,
                    "손실률 -10% 이하에서 손절매했습니다.");
        }
    }

    private void calculateStockRotationRule(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        if (context.isStockRotation()) {
            addRule(appliedRules, BehaviorRuleCode.STOCK_ROTATION, 5, -5, 10,
                    "매도 후 24시간 안에 다른 종목을 매수했습니다.");
        }
    }

    private void calculateMaintainedCashRules(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        if (context.getMaintainedCashRatioDays() == null
                || context.getMaintainedCashRatioDays() < CASH_MAINTENANCE_DAYS
                || context.getCashRatio() == null) {
            return;
        }

        if (context.getCashRatio().compareTo(VERY_LOW_CASH_RATIO) < 0) {
            addRule(appliedRules, BehaviorRuleCode.VERY_LOW_CASH_MAINTENANCE, 15, -15, 5,
                    "현금 비중 5% 미만을 5일 이상 유지했습니다.");
        } else if (context.getCashRatio().compareTo(MEDIUM_CASH_RATIO) >= 0
                && context.getCashRatio().compareTo(HIGH_CASH_RATIO) < 0) {
            addRule(appliedRules, BehaviorRuleCode.CASH_BUFFER_MAINTENANCE, 0, 5, 0,
                    "현금 비중 25% 이상 50% 미만을 5일 이상 유지했습니다.");
        } else if (context.getCashRatio().compareTo(HIGH_CASH_RATIO) >= 0) {
            addRule(appliedRules, BehaviorRuleCode.HIGH_CASH_MAINTENANCE, -10, 15, -5,
                    "현금 비중 50% 이상을 5일 이상 유지했습니다.");
        }
    }

    private void calculateTradeFrequencyRules(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        if (isGreaterThanOrEqual(context.getAverageDailyTradeCount(), HIGH_TRADE_FREQUENCY)) {
            addRule(appliedRules, BehaviorRuleCode.HIGH_TRADE_FREQUENCY, 0, 0, 5,
                    "일평균 거래 횟수가 5회 이상입니다.");
        } else if (isLessThanOrEqual(context.getAverageDailyTradeCount(), LOW_TRADE_FREQUENCY)) {
            addRule(appliedRules, BehaviorRuleCode.LOW_TRADE_FREQUENCY, 0, 0, -5,
                    "일평균 거래 횟수가 0.2회 이하입니다.");
        }
    }

    private List<RuleResult> applyConsecutiveActionMultiplier(
            BehaviorContext context,
            List<RuleResult> appliedRules) {
        if (context.getCurrentEvent() == null
                || context.getConsecutiveActionCount() == null
                || context.getConsecutiveActionCount() <= 1
                || context.getCurrentEvent().getActionType() == BehaviorActionType.INITIAL_ALLOCATION) {
            return appliedRules;
        }

        BigDecimal multiplier = context.getConsecutiveActionCount() == 2
                ? BigDecimal.valueOf(1.2)
                : BigDecimal.valueOf(1.5);

        return appliedRules.stream()
                .map(ruleResult -> ruleResult.multiplyScoreDelta(multiplier))
                .toList();
    }

    private void addRule(
            List<RuleResult> appliedRules,
            BehaviorRuleCode ruleCode,
            int rtDelta,
            int lhDelta,
            int rpDelta,
            String reason) {
        appliedRules.add(new RuleResult(
                ruleCode,
                ScoreDelta.createScoreDelta(rtDelta, lhDelta, rpDelta),
                reason
        ));
    }

    private boolean isGreaterThanOrEqual(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) >= 0;
    }

    private boolean isGreaterThan(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) > 0;
    }

    private boolean isLessThanOrEqual(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) <= 0;
    }

    private boolean isLessThan(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) < 0;
    }
}
