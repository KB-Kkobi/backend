package org.kkobi.assessment.simulation;

import lombok.Getter;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.domain.RuleResult;
import org.kkobi.assessment.domain.ScoreDelta;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.MarketState;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Getter
public enum GameRuleEvaluationCondition {

    BASELINE("기존 규칙", false, false, false, false, false, false),
    EXCLUSIVE_RULE_PRIORITY("구체적인 규칙 우선 적용", true, false, false, false, false, false),
    EXCLUSIVE_PRIORITY_AND_ACTION_RATIO(
            "규칙 우선순위와 거래 비율 적용",
            true,
            true,
            false,
            false,
            false,
            false
    ),
    EXCLUSIVE_RATIO_AND_AXIS_SEPARATION(
            "규칙 우선순위·거래 비율·RT/RP 역할 분리",
            true,
            true,
            true,
            false,
            false,
            false
    ),
    EXCLUSIVE_RATIO_AXIS_AND_DEPOSIT_DECISION(
            "규칙 우선순위·거래 비율·축 분리·예금 해지 후 행동 판정",
            true,
            true,
            true,
            false,
            false,
            true
    ),
    EXCLUSIVE_FIXED_ACTION_SCORE_AND_DEPOSIT_DECISION(
            "규칙 우선순위·정수 거래 비율 점수·예금 해지 후 행동 판정",
            true,
            false,
            false,
            true,
            false,
            true
    ),
    EXCLUSIVE_MODERATE_FIXED_ACTION_SCORE_AND_DEPOSIT_DECISION(
            "규칙 우선순위·중간 정수 거래 비율 점수·예금 해지 후 행동 판정",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    EXCLUSIVE_MODERATE_RP_CENTERED_BULL_BUY_AND_DEPOSIT_DECISION(
            "중간 정수안·급등 매수 RP 중심·예금 해지 후 행동 판정",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    EXCLUSIVE_MODERATE_REDUCED_BULL_BUY_RP_AND_DEPOSIT_DECISION(
            "중간 정수안·급등 매수 RP 완화·예금 해지 후 행동 판정",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    EXCLUSIVE_MODERATE_SIZE_SEPARATED_BULL_BUY_AND_DEPOSIT_DECISION(
            "중간 정수안·급등 매수 규모별 RT/RP 분리·예금 행동 판정",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    EXCLUSIVE_MODERATE_SIZE_SEPARATED_BULL_BUY_WITH_SMALL_TRADE_DEAD_ZONE(
            "중간 정수안·급등 매수 규모별 RT/RP 분리·소규모 거래 점수 제외",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CRASH_HOLDING(
            "소규모 거래 제외·급락 구간 주식 50% 이상 보유 유지",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_NORMAL_BUY(
            "소규모 거래 제외·평범장 중간 규모 계획 매수",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CASH_BUFFER(
            "소규모 거래 제외·현금 완충 비중 3 Tick 유지",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CRASH_PARTIAL_SELL(
            "소규모 거래 제외·급락장 일부 매도 LH·RP 중심",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    RECOMMENDED_GAME_REPETITION_POLICY(
            "게임 반복 제어·급락 일부 매도",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    THREE_CANDIDATE_RULES(
            "급락 보유·평범장 계획 매수·현금 완충 유지",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    RECOMMENDED_REPETITION_POLICY_AND_THREE_CANDIDATE_RULES(
            "게임 반복 제어·급락 보유·평범장 계획 매수·현금 완충 유지",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    OPPORTUNITY_WEIGHTED_REPETITION_POLICY(
            "반복 행동 기회 비율·관측 신뢰도 보정",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    LOG_DIMINISHING_CANDIDATE_RULES(
            "반복 정책·세 후보 규칙 P95 로그 감쇠",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    LOG_DIMINISHING_ALL_REPEATED_RULES(
            "모든 반복 매수·매도와 세 후보 규칙 P95 로그 감쇠",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    LOG_DIMINISHING_RULE_GROUPS(
            "매수·매도·상태 유지 계열별 P95 로그 감쇠",
            true,
            false,
            false,
            true,
            true,
            true
    ),
    LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP(
            "계열별 P95 로그 감쇠·매수매도 2.5회·상태 유지 1.5회분",
            true,
            false,
            false,
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
    private static final BigDecimal DEPOSIT_BUY_AMOUNT_RATIO = BigDecimal.valueOf(50);
    private static final BigDecimal DEPOSIT_CASH_RETENTION_RATIO = BigDecimal.valueOf(80);
    private static final int DEPOSIT_EVALUATION_TICKS = 2;
    private static final int RATIO_SCALE = 4;
    private static final int SCORE_SCALE = 2;

    private final String description;
    private final boolean exclusiveRulePriority;
    private final boolean actionRatioWeight;
    private final boolean axisSeparation;
    private final boolean fixedActionRatioScore;
    private final boolean moderateFixedActionRatioScore;
    private final boolean depositDecisionEvaluation;

    GameRuleEvaluationCondition(
            String description,
            boolean exclusiveRulePriority,
            boolean actionRatioWeight,
            boolean axisSeparation,
            boolean fixedActionRatioScore,
            boolean moderateFixedActionRatioScore,
            boolean depositDecisionEvaluation) {
        this.description = description;
        this.exclusiveRulePriority = exclusiveRulePriority;
        this.actionRatioWeight = actionRatioWeight;
        this.axisSeparation = axisSeparation;
        this.fixedActionRatioScore = fixedActionRatioScore;
        this.moderateFixedActionRatioScore = moderateFixedActionRatioScore;
        this.depositDecisionEvaluation = depositDecisionEvaluation;
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
        if (fixedActionRatioScore) {
            adjustedRules = applyFixedActionRatioScore(
                    behaviorContext,
                    adjustedRules,
                    moderateFixedActionRatioScore
            );
        } else if (actionRatioWeight) {
            adjustedRules = applyActionRatioWeight(behaviorContext, adjustedRules);
        }
        return new BehaviorAnalysisResult(adjustedRules);
    }

    public BehaviorAnalysisResult excludeSmallTradeScores(
            BehaviorContext behaviorContext,
            BehaviorAnalysisResult analysisResult) {
        if (!excludesSmallTradeScores() || behaviorContext.getCurrentEvent() == null) {
            return analysisResult;
        }

        BehaviorEvent behaviorEvent = behaviorContext.getCurrentEvent();
        if (behaviorEvent.getActionType() == BehaviorActionType.BUY
                && calculateBuyRatio(behaviorEvent).compareTo(BUY_SMALL_RATIO) < 0) {
            return new BehaviorAnalysisResult(List.of());
        }
        if (behaviorEvent.getActionType() == BehaviorActionType.SELL
                && calculateSellRatio(behaviorEvent).compareTo(SELL_MINIMUM_RATIO) < 0) {
            return new BehaviorAnalysisResult(List.of());
        }
        return analysisResult;
    }

    public List<BehaviorAnalysisResult> adjustDepositDecisionResults(
            List<BehaviorContext> behaviorContexts,
            List<BehaviorAnalysisResult> analysisResults) {
        if (!depositDecisionEvaluation) {
            return analysisResults;
        }
        if (behaviorContexts.size() != analysisResults.size()) {
            throw new IllegalArgumentException("행동 조건과 분석 결과의 개수가 일치해야 합니다.");
        }

        int depositCancelIndex = findDepositCancelIndex(behaviorContexts);
        if (depositCancelIndex < 0) {
            return analysisResults;
        }

        List<List<RuleResult>> adjustedRulesByIndex = analysisResults.stream()
                .map(BehaviorAnalysisResult::getAppliedRules)
                .map(rules -> (List<RuleResult>) new ArrayList<>(rules))
                .map(rules -> {
                    rules.removeIf(ruleResult -> ruleResult.getRuleCode()
                            == BehaviorRuleCode.DEPOSIT_CANCEL_AND_SECURITY_BUY);
                    return rules;
                })
                .toList();
        BehaviorEvent depositCancelEvent = behaviorContexts.get(depositCancelIndex)
                .getCurrentEvent();
        List<Integer> evaluationIndexes = findDepositEvaluationIndexes(
                behaviorContexts,
                depositCancelEvent.getGameTick()
        );

        int stockBuyIndex = findDepositFundStockBuyIndex(
                behaviorContexts,
                evaluationIndexes,
                depositCancelEvent.getActionAmount()
        );
        if (stockBuyIndex >= 0) {
            adjustedRulesByIndex.get(stockBuyIndex).add(createDepositCancelAndBuyRule());
        } else if (isDepositCashRetained(
                behaviorContexts,
                evaluationIndexes,
                depositCancelEvent.getCurrentCash()
        )) {
            adjustedRulesByIndex.get(depositCancelIndex).add(createDepositCashRetentionRule());
        }

        return adjustedRulesByIndex.stream()
                .map(BehaviorAnalysisResult::new)
                .toList();
    }

    private int findDepositCancelIndex(List<BehaviorContext> behaviorContexts) {
        for (int index = 0; index < behaviorContexts.size(); index++) {
            BehaviorEvent behaviorEvent = behaviorContexts.get(index).getCurrentEvent();
            if (behaviorEvent != null
                    && behaviorEvent.getActionType() == BehaviorActionType.CANCEL_PRODUCT) {
                return index;
            }
        }
        return -1;
    }

    private List<Integer> findDepositEvaluationIndexes(
            List<BehaviorContext> behaviorContexts,
            Integer depositCancelTick) {
        if (depositCancelTick == null) {
            return List.of();
        }
        List<Integer> evaluationIndexes = new ArrayList<>();
        int lastEvaluationTick = depositCancelTick + DEPOSIT_EVALUATION_TICKS;
        for (int index = 0; index < behaviorContexts.size(); index++) {
            BehaviorEvent behaviorEvent = behaviorContexts.get(index).getCurrentEvent();
            if (behaviorEvent == null || behaviorEvent.getGameTick() == null) {
                continue;
            }
            if (behaviorEvent.getGameTick() >= depositCancelTick
                    && behaviorEvent.getGameTick() <= lastEvaluationTick) {
                evaluationIndexes.add(index);
            }
        }
        return evaluationIndexes;
    }

    private int findDepositFundStockBuyIndex(
            List<BehaviorContext> behaviorContexts,
            List<Integer> evaluationIndexes,
            Long depositCancelAmount) {
        if (depositCancelAmount == null || depositCancelAmount <= 0) {
            return -1;
        }
        long accumulatedBuyAmount = 0L;
        for (Integer index : evaluationIndexes) {
            BehaviorEvent behaviorEvent = behaviorContexts.get(index).getCurrentEvent();
            if (behaviorEvent.getActionType() != BehaviorActionType.BUY
                    || behaviorEvent.getAssetType() != BehaviorAssetType.SECURITY
                    || behaviorEvent.getActionAmount() == null
                    || isExcludedSmallBuy(behaviorEvent)) {
                continue;
            }
            accumulatedBuyAmount = Math.addExact(
                    accumulatedBuyAmount,
                    behaviorEvent.getActionAmount()
            );
            BigDecimal buyAmountRatio = calculateRatio(
                    accumulatedBuyAmount,
                    depositCancelAmount
            );
            if (buyAmountRatio.compareTo(DEPOSIT_BUY_AMOUNT_RATIO) >= 0) {
                return index;
            }
        }
        return -1;
    }

    private boolean isDepositCashRetained(
            List<BehaviorContext> behaviorContexts,
            List<Integer> evaluationIndexes,
            Long cashAfterDepositCancel) {
        if (cashAfterDepositCancel == null || cashAfterDepositCancel <= 0) {
            return false;
        }
        long minimumCash = cashAfterDepositCancel;
        for (Integer index : evaluationIndexes) {
            Long currentCash = behaviorContexts.get(index).getCurrentEvent().getCurrentCash();
            if (currentCash != null) {
                minimumCash = Math.min(minimumCash, currentCash);
            }
        }
        return calculateRatio(minimumCash, cashAfterDepositCancel)
                .compareTo(DEPOSIT_CASH_RETENTION_RATIO) >= 0;
    }

    private RuleResult createDepositCancelAndBuyRule() {
        return new RuleResult(
                BehaviorRuleCode.DEPOSIT_CANCEL_AND_SECURITY_BUY,
                ScoreDelta.createScoreDelta(5, -10, 5),
                "예금 해지 후 2 Tick 안에 해지 금액의 50% 이상을 주식 매수에 사용했습니다."
        );
    }

    private RuleResult createDepositCashRetentionRule() {
        return new RuleResult(
                BehaviorRuleCode.DEPOSIT_CANCEL_AND_SECURITY_BUY,
                ScoreDelta.createScoreDelta(-5, 10, -5),
                "예금 해지 후 2 Tick 동안 해지 직후 현금의 80% 이상을 유지했습니다."
        );
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

    private List<RuleResult> applyFixedActionRatioScore(
            BehaviorContext behaviorContext,
            List<RuleResult> appliedRules,
            boolean moderateScore) {
        BehaviorActionType actionType = behaviorContext.getCurrentEvent().getActionType();
        if (actionType == BehaviorActionType.BUY) {
            return adjustFixedBuyRules(behaviorContext, appliedRules, moderateScore);
        }
        if (actionType == BehaviorActionType.SELL) {
            return adjustFixedSellRules(behaviorContext, appliedRules, moderateScore);
        }
        return appliedRules;
    }

    private List<RuleResult> adjustFixedBuyRules(
            BehaviorContext behaviorContext,
            List<RuleResult> appliedRules,
            boolean moderateScore) {
        BigDecimal buyRatio = calculateBuyRatio(behaviorContext.getCurrentEvent());
        return appliedRules.stream()
                .map(ruleResult -> adjustFixedBuyRule(ruleResult, buyRatio, moderateScore))
                .toList();
    }

    private RuleResult adjustFixedBuyRule(
            RuleResult ruleResult,
            BigDecimal buyRatio,
            boolean moderateScore) {
        ScoreDelta scoreDelta = switch (ruleResult.getRuleCode()) {
            case CRASH_BUY -> calculateFixedCrashBuyScore(buyRatio, moderateScore);
            case BULL_BUY -> calculateFixedBullBuyScore(buyRatio, moderateScore);
            case LOSS_AVERAGING_BUY -> calculateFixedLossAveragingBuyScore(buyRatio);
            default -> ruleResult.getScoreDelta();
        };
        return replaceScoreDelta(ruleResult, scoreDelta);
    }

    private ScoreDelta calculateFixedCrashBuyScore(
            BigDecimal buyRatio,
            boolean moderateScore) {
        if (buyRatio.compareTo(BUY_SMALL_RATIO) < 0) {
            return ScoreDelta.createScoreDelta(5, 0, 0);
        }
        if (buyRatio.compareTo(BUY_LARGE_RATIO) >= 0) {
            return ScoreDelta.createScoreDelta(15, -10, moderateScore ? 0 : 5);
        }
        return ScoreDelta.createScoreDelta(10, -5, moderateScore ? 0 : 5);
    }

    private ScoreDelta calculateFixedBullBuyScore(
            BigDecimal buyRatio,
            boolean moderateScore) {
        if (this == EXCLUSIVE_MODERATE_RP_CENTERED_BULL_BUY_AND_DEPOSIT_DECISION) {
            return calculateRpCenteredBullBuyScore(buyRatio);
        }
        if (this == EXCLUSIVE_MODERATE_REDUCED_BULL_BUY_RP_AND_DEPOSIT_DECISION) {
            return calculateReducedRpBullBuyScore(buyRatio);
        }
        if (isSizeSeparatedBullBuyCondition()) {
            return calculateSizeSeparatedBullBuyScore(buyRatio);
        }
        if (buyRatio.compareTo(BUY_SMALL_RATIO) < 0) {
            return ScoreDelta.createScoreDelta(5, 0, 5);
        }
        if (buyRatio.compareTo(BUY_LARGE_RATIO) >= 0) {
            return ScoreDelta.createScoreDelta(10, -10, moderateScore ? 10 : 15);
        }
        return ScoreDelta.createScoreDelta(5, -5, 10);
    }

    private ScoreDelta calculateRpCenteredBullBuyScore(BigDecimal buyRatio) {
        if (buyRatio.compareTo(BUY_SMALL_RATIO) < 0) {
            return ScoreDelta.createScoreDelta(0, 0, 5);
        }
        if (buyRatio.compareTo(BUY_LARGE_RATIO) >= 0) {
            return ScoreDelta.createScoreDelta(5, -10, 10);
        }
        return ScoreDelta.createScoreDelta(0, -5, 10);
    }

    private ScoreDelta calculateReducedRpBullBuyScore(BigDecimal buyRatio) {
        if (buyRatio.compareTo(BUY_SMALL_RATIO) < 0) {
            return ScoreDelta.createScoreDelta(5, 0, 5);
        }
        if (buyRatio.compareTo(BUY_LARGE_RATIO) >= 0) {
            return ScoreDelta.createScoreDelta(10, -10, 5);
        }
        return ScoreDelta.createScoreDelta(5, -5, 5);
    }

    private ScoreDelta calculateSizeSeparatedBullBuyScore(BigDecimal buyRatio) {
        if (buyRatio.compareTo(BUY_SMALL_RATIO) < 0) {
            return ScoreDelta.createScoreDelta(0, 0, 5);
        }
        if (buyRatio.compareTo(BUY_LARGE_RATIO) >= 0) {
            return ScoreDelta.createScoreDelta(10, -10, 5);
        }
        return ScoreDelta.createScoreDelta(0, -5, 10);
    }

    private ScoreDelta calculateFixedLossAveragingBuyScore(BigDecimal buyRatio) {
        if (buyRatio.compareTo(BUY_SMALL_RATIO) < 0) {
            return ScoreDelta.createScoreDelta(5, 0, 0);
        }
        if (buyRatio.compareTo(BUY_LARGE_RATIO) >= 0) {
            return ScoreDelta.createScoreDelta(15, -10, 0);
        }
        return ScoreDelta.createScoreDelta(10, -5, 0);
    }

    private List<RuleResult> adjustFixedSellRules(
            BehaviorContext behaviorContext,
            List<RuleResult> appliedRules,
            boolean moderateScore) {
        BigDecimal sellRatio = calculateSellRatio(behaviorContext.getCurrentEvent());
        List<RuleResult> adjustedRules = appliedRules.stream()
                .map(ruleResult -> adjustFixedSellRule(ruleResult, sellRatio, moderateScore))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        if (canAddCrashPartialSellRule(behaviorContext, adjustedRules, sellRatio)) {
            adjustedRules.add(createFixedCrashPartialSellRule(sellRatio));
        }
        return adjustedRules;
    }

    private RuleResult adjustFixedSellRule(
            RuleResult ruleResult,
            BigDecimal sellRatio,
            boolean moderateScore) {
        ScoreDelta scoreDelta = switch (ruleResult.getRuleCode()) {
            case CRASH_FULL_SELL -> ScoreDelta.createScoreDelta(-15, 10, -5);
            case LOSS_CUT_SELL -> calculateFixedLossCutSellScore(sellRatio, moderateScore);
            case BULL_PROFIT_SELL -> calculateFixedBullProfitSellScore(sellRatio, moderateScore);
            default -> ruleResult.getScoreDelta();
        };
        return replaceScoreDelta(ruleResult, scoreDelta);
    }

    private ScoreDelta calculateFixedLossCutSellScore(
            BigDecimal sellRatio,
            boolean moderateScore) {
        if (sellRatio.compareTo(SELL_MINIMUM_RATIO) < 0) {
            return ScoreDelta.createScoreDelta(-5, 5, 0);
        }
        if (sellRatio.compareTo(SELL_MAJORITY_RATIO) < 0) {
            return moderateScore
                    ? ScoreDelta.createScoreDelta(-5, 5, -5)
                    : ScoreDelta.createScoreDelta(-10, 10, -5);
        }
        if (sellRatio.compareTo(BigDecimal.valueOf(100)) < 0) {
            return moderateScore
                    ? ScoreDelta.createScoreDelta(-10, 10, -5)
                    : ScoreDelta.createScoreDelta(-15, 15, -10);
        }
        return moderateScore
                ? ScoreDelta.createScoreDelta(-15, 10, -10)
                : ScoreDelta.createScoreDelta(-15, 15, -10);
    }

    private ScoreDelta calculateFixedBullProfitSellScore(
            BigDecimal sellRatio,
            boolean moderateScore) {
        if (sellRatio.compareTo(SELL_MINIMUM_RATIO) < 0) {
            return ScoreDelta.createScoreDelta(0, 5, 0);
        }
        if (sellRatio.compareTo(SELL_MAJORITY_RATIO) < 0) {
            return moderateScore
                    ? ScoreDelta.createScoreDelta(0, 5, 5)
                    : ScoreDelta.createScoreDelta(0, 10, 5);
        }
        return moderateScore
                ? ScoreDelta.createScoreDelta(0, 10, 0)
                : ScoreDelta.createScoreDelta(0, 15, 10);
    }

    private RuleResult createFixedCrashPartialSellRule(BigDecimal sellRatio) {
        ScoreDelta scoreDelta = sellRatio.compareTo(SELL_MAJORITY_RATIO) >= 0
                ? ScoreDelta.createScoreDelta(-10, 5, -5)
                : appliesCrashPartialSellAxisSeparation()
                        ? ScoreDelta.createScoreDelta(0, 5, -5)
                        : ScoreDelta.createScoreDelta(-5, 5, 0);
        return new RuleResult(
                BehaviorRuleCode.CRASH_FULL_SELL,
                scoreDelta,
                "급락 상황에서 보유 증권을 일부 매도했습니다."
        );
    }

    private RuleResult replaceScoreDelta(RuleResult ruleResult, ScoreDelta scoreDelta) {
        return new RuleResult(
                ruleResult.getRuleCode(),
                scoreDelta,
                ruleResult.getReason()
        );
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

    private boolean excludesSmallTradeScores() {
        return this == EXCLUSIVE_MODERATE_SIZE_SEPARATED_BULL_BUY_WITH_SMALL_TRADE_DEAD_ZONE
                || this == EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CRASH_HOLDING
                || this == EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_NORMAL_BUY
                || this == EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CASH_BUFFER
                || this == EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CRASH_PARTIAL_SELL
                || this == RECOMMENDED_GAME_REPETITION_POLICY
                || this == THREE_CANDIDATE_RULES
                || this == RECOMMENDED_REPETITION_POLICY_AND_THREE_CANDIDATE_RULES
                || this == OPPORTUNITY_WEIGHTED_REPETITION_POLICY
                || this == LOG_DIMINISHING_CANDIDATE_RULES
                || this == LOG_DIMINISHING_ALL_REPEATED_RULES
                || this == LOG_DIMINISHING_RULE_GROUPS
                || this == LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP;
    }

    private boolean isSizeSeparatedBullBuyCondition() {
        return this == EXCLUSIVE_MODERATE_SIZE_SEPARATED_BULL_BUY_AND_DEPOSIT_DECISION
                || excludesSmallTradeScores();
    }

    public boolean appliesCrashHoldingRule() {
        return this
                == EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CRASH_HOLDING
                || this == THREE_CANDIDATE_RULES
                || this == RECOMMENDED_REPETITION_POLICY_AND_THREE_CANDIDATE_RULES
                || this == LOG_DIMINISHING_CANDIDATE_RULES
                || this == LOG_DIMINISHING_ALL_REPEATED_RULES
                || this == LOG_DIMINISHING_RULE_GROUPS
                || this == LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP;
    }

    public boolean appliesNormalPlannedBuyRule() {
        return this
                == EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_NORMAL_BUY
                || this == THREE_CANDIDATE_RULES
                || this == RECOMMENDED_REPETITION_POLICY_AND_THREE_CANDIDATE_RULES
                || this == LOG_DIMINISHING_CANDIDATE_RULES
                || this == LOG_DIMINISHING_ALL_REPEATED_RULES
                || this == LOG_DIMINISHING_RULE_GROUPS
                || this == LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP;
    }

    public boolean appliesCashBufferMaintenanceRule() {
        return this
                == EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CASH_BUFFER
                || this == THREE_CANDIDATE_RULES
                || this == RECOMMENDED_REPETITION_POLICY_AND_THREE_CANDIDATE_RULES
                || this == LOG_DIMINISHING_CANDIDATE_RULES
                || this == LOG_DIMINISHING_ALL_REPEATED_RULES
                || this == LOG_DIMINISHING_RULE_GROUPS
                || this == LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP;
    }

    public boolean appliesCashBufferMaintenancePerEpisodeRule() {
        return this
                == EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CASH_BUFFER
                || this == THREE_CANDIDATE_RULES
                || this == RECOMMENDED_REPETITION_POLICY_AND_THREE_CANDIDATE_RULES
                || this == LOG_DIMINISHING_CANDIDATE_RULES
                || this == LOG_DIMINISHING_ALL_REPEATED_RULES
                || this == LOG_DIMINISHING_RULE_GROUPS
                || this == LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP;
    }

    private boolean appliesCrashPartialSellAxisSeparation() {
        return this
                == EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CRASH_PARTIAL_SELL
                || this == RECOMMENDED_GAME_REPETITION_POLICY
                || this == THREE_CANDIDATE_RULES
                || this == RECOMMENDED_REPETITION_POLICY_AND_THREE_CANDIDATE_RULES
                || this == OPPORTUNITY_WEIGHTED_REPETITION_POLICY
                || this == LOG_DIMINISHING_CANDIDATE_RULES
                || this == LOG_DIMINISHING_ALL_REPEATED_RULES
                || this == LOG_DIMINISHING_RULE_GROUPS
                || this == LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP;
    }

    public boolean appliesOpportunityWeightedRepeatedScore() {
        return this == OPPORTUNITY_WEIGHTED_REPETITION_POLICY;
    }

    public boolean appliesLogDiminishingCandidateScore() {
        return this == LOG_DIMINISHING_CANDIDATE_RULES
                || this == LOG_DIMINISHING_ALL_REPEATED_RULES;
    }

    public boolean appliesLogDiminishingRepeatedRuleScore() {
        return this == LOG_DIMINISHING_ALL_REPEATED_RULES;
    }

    public boolean appliesLogDiminishingRuleGroupScore() {
        return this == LOG_DIMINISHING_RULE_GROUPS
                || this == LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP;
    }

    public boolean appliesBalancedRuleGroupMaximum() {
        return this == LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP;
    }

    private boolean isExcludedSmallBuy(BehaviorEvent behaviorEvent) {
        return excludesSmallTradeScores()
                && calculateBuyRatio(behaviorEvent).compareTo(BUY_SMALL_RATIO) < 0;
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
