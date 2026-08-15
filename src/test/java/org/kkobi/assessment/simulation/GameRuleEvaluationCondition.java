package org.kkobi.assessment.simulation;

import lombok.Getter;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.domain.RuleResult;
import org.kkobi.assessment.domain.ScoreDelta;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.MarketState;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Getter
public enum GameRuleEvaluationCondition {

    BASELINE("기존 규칙", false, false, false),
    EXCLUSIVE_RULE_PRIORITY("구체적인 규칙 우선 적용", true, false, false),
    EXCLUSIVE_PRIORITY_AND_ACTION_RATIO(
            "규칙 우선순위와 거래 비율 적용",
            true,
            true,
            false
    ),
    EXCLUSIVE_RATIO_AND_AXIS_SEPARATION(
            "규칙 우선순위·거래 비율·RT/RP 역할 분리",
            true,
            true,
            true
    );

    private static final BigDecimal BUY_SMALL_RATIO = BigDecimal.valueOf(10);
    private static final BigDecimal BUY_LARGE_RATIO = BigDecimal.valueOf(30);
    private static final BigDecimal SELL_MINIMUM_RATIO = BigDecimal.valueOf(20);
    private static final BigDecimal SELL_MAJORITY_RATIO = BigDecimal.valueOf(50);
    private static final BigDecimal SMALL_ACTION_MULTIPLIER = BigDecimal.valueOf(0.5);
    private static final BigDecimal LARGE_ACTION_MULTIPLIER = BigDecimal.valueOf(1.25);
    private static final BigDecimal LOSS_AVERAGING_RT_MULTIPLIER =
            BigDecimal.valueOf(10).divide(BigDecimal.valueOf(15), 4, RoundingMode.HALF_UP);
    private static final int RATIO_SCALE = 4;
    private static final int SCORE_SCALE = 2;

    private final String description;
    private final boolean exclusiveRulePriority;
    private final boolean actionRatioWeight;
    private final boolean axisSeparation;

    GameRuleEvaluationCondition(
            String description,
            boolean exclusiveRulePriority,
            boolean actionRatioWeight,
            boolean axisSeparation) {
        this.description = description;
        this.exclusiveRulePriority = exclusiveRulePriority;
        this.actionRatioWeight = actionRatioWeight;
        this.axisSeparation = axisSeparation;
    }

    public BehaviorAnalysisResult adjustAnalysisResult(
            BehaviorContext behaviorContext,
            BehaviorAnalysisResult analysisResult) {
        if (this == BASELINE || behaviorContext.getCurrentEvent() == null) {
            return analysisResult;
        }

        List<RuleResult> adjustedRules = applyExclusiveRulePriority(
                behaviorContext,
                analysisResult.getAppliedRules()
        );
        if (axisSeparation) {
            adjustedRules = applyAxisSeparation(adjustedRules);
        }
        if (actionRatioWeight) {
            adjustedRules = applyActionRatioWeight(behaviorContext, adjustedRules);
        }
        return new BehaviorAnalysisResult(adjustedRules);
    }

    private List<RuleResult> applyExclusiveRulePriority(
            BehaviorContext behaviorContext,
            List<RuleResult> appliedRules) {
        BehaviorEvent behaviorEvent = behaviorContext.getCurrentEvent();
        List<RuleResult> adjustedRules = new ArrayList<>(appliedRules);

        if (behaviorEvent.getActionType() == BehaviorActionType.BUY
                && containsRule(adjustedRules, BehaviorRuleCode.LOSS_AVERAGING_BUY)) {
            adjustedRules.removeIf(ruleResult ->
                    ruleResult.getRuleCode() == BehaviorRuleCode.CRASH_BUY
                            || ruleResult.getRuleCode() == BehaviorRuleCode.BULL_BUY);
        }
        if (behaviorEvent.getActionType() == BehaviorActionType.SELL
                && behaviorContext.getMarketState() == MarketState.CRASH
                && behaviorContext.isFullSecuritySell()) {
            adjustedRules.removeIf(ruleResult ->
                    ruleResult.getRuleCode() == BehaviorRuleCode.LOSS_CUT_SELL);
        }
        return adjustedRules;
    }

    private List<RuleResult> applyAxisSeparation(List<RuleResult> appliedRules) {
        return appliedRules.stream()
                .map(this::adjustAxisSeparationRule)
                .toList();
    }

    private RuleResult adjustAxisSeparationRule(RuleResult ruleResult) {
        ScoreDelta scoreDelta = ruleResult.getScoreDelta();
        ScoreDelta adjustedScoreDelta = switch (ruleResult.getRuleCode()) {
            case CRASH_BUY -> new ScoreDelta(
                    scoreDelta.getRtDelta(),
                    scoreDelta.getLhDelta(),
                    BigDecimal.ZERO
            );
            case LOSS_AVERAGING_BUY -> new ScoreDelta(
                    multiplyScore(scoreDelta.getRtDelta(), LOSS_AVERAGING_RT_MULTIPLIER),
                    scoreDelta.getLhDelta(),
                    BigDecimal.ZERO
            );
            case DEPOSIT_CANCEL_AND_SECURITY_BUY -> new ScoreDelta(
                    scoreDelta.getRtDelta(),
                    scoreDelta.getLhDelta(),
                    multiplyScore(scoreDelta.getRpDelta(), BigDecimal.valueOf(0.5))
            );
            default -> scoreDelta;
        };
        return new RuleResult(
                ruleResult.getRuleCode(),
                adjustedScoreDelta,
                ruleResult.getReason()
        );
    }

    private List<RuleResult> applyActionRatioWeight(
            BehaviorContext behaviorContext,
            List<RuleResult> appliedRules) {
        BehaviorActionType actionType = behaviorContext.getCurrentEvent().getActionType();
        if (actionType == BehaviorActionType.BUY) {
            return adjustBuyRules(behaviorContext, appliedRules);
        }
        if (actionType == BehaviorActionType.SELL) {
            return adjustSellRules(behaviorContext, appliedRules);
        }
        return appliedRules;
    }

    private List<RuleResult> adjustBuyRules(
            BehaviorContext behaviorContext,
            List<RuleResult> appliedRules) {
        BigDecimal buyRatio = calculateBuyRatio(behaviorContext.getCurrentEvent());
        BigDecimal multiplier = calculateBuyMultiplier(buyRatio);

        return appliedRules.stream()
                .map(ruleResult -> isMarketBuyRule(ruleResult.getRuleCode())
                        ? ruleResult.multiplyScoreDelta(multiplier)
                        : ruleResult)
                .toList();
    }

    private List<RuleResult> adjustSellRules(
            BehaviorContext behaviorContext,
            List<RuleResult> appliedRules) {
        BigDecimal sellRatio = calculateSellRatio(behaviorContext.getCurrentEvent());
        List<RuleResult> adjustedRules = appliedRules.stream()
                .map(ruleResult -> isReturnSellRule(ruleResult.getRuleCode())
                        ? ruleResult.multiplyScoreDelta(calculateSellMultiplier(sellRatio))
                        : ruleResult)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        if (canAddCrashPartialSellRule(behaviorContext, adjustedRules, sellRatio)) {
            adjustedRules.add(createCrashPartialSellRule(behaviorContext, sellRatio));
        }
        return adjustedRules;
    }

    private boolean canAddCrashPartialSellRule(
            BehaviorContext behaviorContext,
            List<RuleResult> appliedRules,
            BigDecimal sellRatio) {
        return behaviorContext.getMarketState() == MarketState.CRASH
                && !behaviorContext.isFullSecuritySell()
                && sellRatio.compareTo(SELL_MINIMUM_RATIO) >= 0
                && !containsRule(appliedRules, BehaviorRuleCode.LOSS_CUT_SELL);
    }

    private RuleResult createCrashPartialSellRule(
            BehaviorContext behaviorContext,
            BigDecimal sellRatio) {
        ScoreDelta scoreDelta = sellRatio.compareTo(SELL_MAJORITY_RATIO) >= 0
                ? ScoreDelta.createScoreDelta(-10, 5, -5)
                : ScoreDelta.createScoreDelta(-5, 5, 0);
        return new RuleResult(
                BehaviorRuleCode.CRASH_FULL_SELL,
                scoreDelta.multiplyScoreDelta(calculateConsecutiveActionMultiplier(behaviorContext)),
                "급락 상황에서 보유 증권을 일부 매도했습니다."
        );
    }

    private BigDecimal calculateBuyRatio(BehaviorEvent behaviorEvent) {
        long totalAssetPrincipal = addAmounts(
                addAmounts(behaviorEvent.getCurrentCash(), behaviorEvent.getCurrentStockPrincipal()),
                behaviorEvent.getCurrentDeposit()
        );
        return calculateRatio(behaviorEvent.getActionAmount(), totalAssetPrincipal);
    }

    private BigDecimal calculateSellRatio(BehaviorEvent behaviorEvent) {
        if (behaviorEvent.getQuantity() == null
                || behaviorEvent.getCurrentSecurityQuantity() == null) {
            return BigDecimal.ZERO;
        }
        long quantityBeforeSell = (long) behaviorEvent.getQuantity()
                + behaviorEvent.getCurrentSecurityQuantity();
        return calculateRatio(behaviorEvent.getQuantity(), quantityBeforeSell);
    }

    private BigDecimal calculateRatio(long numerator, long denominator) {
        if (numerator <= 0 || denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBuyMultiplier(BigDecimal buyRatio) {
        if (buyRatio.compareTo(BUY_SMALL_RATIO) < 0) {
            return SMALL_ACTION_MULTIPLIER;
        }
        if (buyRatio.compareTo(BUY_LARGE_RATIO) >= 0) {
            return LARGE_ACTION_MULTIPLIER;
        }
        return BigDecimal.ONE;
    }

    private BigDecimal calculateSellMultiplier(BigDecimal sellRatio) {
        if (sellRatio.compareTo(SELL_MINIMUM_RATIO) < 0) {
            return SMALL_ACTION_MULTIPLIER;
        }
        if (sellRatio.compareTo(SELL_MAJORITY_RATIO) >= 0
                && sellRatio.compareTo(BigDecimal.valueOf(100)) < 0) {
            return LARGE_ACTION_MULTIPLIER;
        }
        return BigDecimal.ONE;
    }

    private BigDecimal calculateConsecutiveActionMultiplier(BehaviorContext behaviorContext) {
        if (behaviorContext.getConsecutiveActionCount() == null
                || behaviorContext.getConsecutiveActionCount() <= 1) {
            return BigDecimal.ONE;
        }
        return behaviorContext.getConsecutiveActionCount() == 2
                ? BigDecimal.valueOf(1.2)
                : BigDecimal.valueOf(1.5);
    }

    private boolean isMarketBuyRule(BehaviorRuleCode ruleCode) {
        return ruleCode == BehaviorRuleCode.CRASH_BUY
                || ruleCode == BehaviorRuleCode.BULL_BUY
                || ruleCode == BehaviorRuleCode.LOSS_AVERAGING_BUY;
    }

    private boolean isReturnSellRule(BehaviorRuleCode ruleCode) {
        return ruleCode == BehaviorRuleCode.LOSS_CUT_SELL
                || ruleCode == BehaviorRuleCode.BULL_PROFIT_SELL;
    }

    private boolean containsRule(
            List<RuleResult> appliedRules,
            BehaviorRuleCode ruleCode) {
        return appliedRules.stream()
                .anyMatch(ruleResult -> ruleResult.getRuleCode() == ruleCode);
    }

    private long addAmounts(Long firstAmount, Long secondAmount) {
        if (firstAmount == null || secondAmount == null) {
            return 0L;
        }
        try {
            return Math.addExact(firstAmount, secondAmount);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("시뮬레이션 자산 금액이 허용 범위를 초과했습니다.", exception);
        }
    }

    private BigDecimal multiplyScore(BigDecimal score, BigDecimal multiplier) {
        return score.multiply(multiplier).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }
}
