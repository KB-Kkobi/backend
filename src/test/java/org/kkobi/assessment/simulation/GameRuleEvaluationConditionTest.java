package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRuleEvaluationConditionTest {

    @Test
    @DisplayName("급락장 물타기 매수는 물타기 규칙만 적용한다.")
    void prioritizeLossAveragingRuleOverCrashBuyRule() {
        BehaviorContext behaviorContext = createBuyContext(2_000_000L, 10_000_000L);
        BehaviorAnalysisResult analysisResult = createAnalysisResult(
                createRule(BehaviorRuleCode.CRASH_BUY, 10, -5, 5),
                createRule(BehaviorRuleCode.LOSS_AVERAGING_BUY, 15, -5, 5)
        );

        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_RULE_PRIORITY
                .adjustAnalysisResult(behaviorContext, analysisResult);

        assertFalse(containsRule(adjustedResult, BehaviorRuleCode.CRASH_BUY));
        assertTrue(containsRule(adjustedResult, BehaviorRuleCode.LOSS_AVERAGING_BUY));
        assertScore(adjustedResult, "15", "-5", "5");
    }

    @Test
    @DisplayName("급락장 전량 손절은 전량 매도 규칙만 적용한다.")
    void prioritizeCrashFullSellRuleOverLossCutRule() {
        BehaviorContext behaviorContext = createSellContext(100, 0, true);
        BehaviorAnalysisResult analysisResult = createAnalysisResult(
                createRule(BehaviorRuleCode.CRASH_FULL_SELL, -15, 10, -5),
                createRule(BehaviorRuleCode.LOSS_CUT_SELL, -5, 5, -5)
        );

        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_RULE_PRIORITY
                .adjustAnalysisResult(behaviorContext, analysisResult);

        assertTrue(containsRule(adjustedResult, BehaviorRuleCode.CRASH_FULL_SELL));
        assertFalse(containsRule(adjustedResult, BehaviorRuleCode.LOSS_CUT_SELL));
        assertScore(adjustedResult, "-15", "10", "-5");
    }

    @Test
    @DisplayName("급락장에서 보유 수량의 30%를 매도하면 일부 매도 점수를 적용한다.")
    void applyCrashPartialSellRule() {
        BehaviorContext behaviorContext = createSellContext(30, 70, false);

        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_PRIORITY_AND_ACTION_RATIO
                .adjustAnalysisResult(behaviorContext, createAnalysisResult());

        assertTrue(containsRule(adjustedResult, BehaviorRuleCode.CRASH_FULL_SELL));
        assertScore(adjustedResult, "-5", "5", "0");
    }

    @Test
    @DisplayName("총자산의 10% 미만 매수는 시장 매수 점수를 절반만 반영한다.")
    void reduceSmallBuyRuleWeight() {
        BehaviorContext behaviorContext = createBuyContext(900_000L, 10_000_000L);
        BehaviorAnalysisResult analysisResult = createAnalysisResult(
                createRule(BehaviorRuleCode.BULL_BUY, 5, -5, 10)
        );

        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_PRIORITY_AND_ACTION_RATIO
                .adjustAnalysisResult(behaviorContext, analysisResult);

        assertScore(adjustedResult, "2.50", "-2.50", "5.00");
    }

    @Test
    @DisplayName("총자산의 30% 이상 매수는 시장 매수 점수를 1.25배 반영한다.")
    void increaseLargeBuyRuleWeight() {
        BehaviorContext behaviorContext = createBuyContext(3_000_000L, 10_000_000L);
        BehaviorAnalysisResult analysisResult = createAnalysisResult(
                createRule(BehaviorRuleCode.CRASH_BUY, 10, -5, 5)
        );

        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_PRIORITY_AND_ACTION_RATIO
                .adjustAnalysisResult(behaviorContext, analysisResult);

        assertScore(adjustedResult, "12.50", "-6.25", "6.25");
    }

    @Test
    @DisplayName("급락장 매수는 RT를 유지하고 RP 기여를 제거한다.")
    void separateCrashBuyRtAndRpContribution() {
        BehaviorContext behaviorContext = createBuyContext(2_000_000L, 10_000_000L);
        BehaviorAnalysisResult analysisResult = createAnalysisResult(
                createRule(BehaviorRuleCode.CRASH_BUY, 10, -5, 5)
        );

        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_RATIO_AND_AXIS_SEPARATION
                .adjustAnalysisResult(behaviorContext, analysisResult);

        assertScore(adjustedResult, "10", "-5", "0");
    }

    @Test
    @DisplayName("물타기 매수는 RT를 10점으로 낮추고 RP 기여를 제거한다.")
    void separateLossAveragingRtAndRpContribution() {
        BehaviorContext behaviorContext = createBuyContext(2_000_000L, 10_000_000L);
        BehaviorAnalysisResult analysisResult = createAnalysisResult(
                createRule(BehaviorRuleCode.LOSS_AVERAGING_BUY, 15, -5, 5)
        );

        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_RATIO_AND_AXIS_SEPARATION
                .adjustAnalysisResult(behaviorContext, analysisResult);

        assertScore(adjustedResult, "10.00", "-5", "0");
    }

    @Test
    @DisplayName("급등장 매수의 RT·LH·RP 가중치는 유지한다.")
    void maintainBullBuyContribution() {
        BehaviorContext behaviorContext = createBuyContext(2_000_000L, 10_000_000L);
        BehaviorAnalysisResult analysisResult = createAnalysisResult(
                createRule(BehaviorRuleCode.BULL_BUY, 5, -5, 10)
        );

        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_RATIO_AND_AXIS_SEPARATION
                .adjustAnalysisResult(behaviorContext, analysisResult);

        assertScore(adjustedResult, "5", "-5", "10");
    }

    @Test
    @DisplayName("예금 해지 후 매수의 RP 가중치를 5점으로 낮춘다.")
    void reduceDepositCancelAndBuyRpContribution() {
        BehaviorContext behaviorContext = createBuyContext(2_000_000L, 10_000_000L);
        BehaviorAnalysisResult analysisResult = createAnalysisResult(
                createRule(BehaviorRuleCode.DEPOSIT_CANCEL_AND_SECURITY_BUY, 5, -10, 10)
        );

        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_RATIO_AND_AXIS_SEPARATION
                .adjustAnalysisResult(behaviorContext, analysisResult);

        assertScore(adjustedResult, "5", "-10", "5.00");
    }

    @Test
    @DisplayName("예금 해지 후 2 Tick 안에 해지액의 50% 이상을 매수하면 매수 규칙만 적용한다.")
    void applyDepositCancelAndBuyRuleExclusively() {
        BehaviorContext cancelContext = createDepositActionContext(
                BehaviorActionType.CANCEL_PRODUCT,
                10,
                1_000_000L,
                3_000_000L
        );
        BehaviorContext buyContext = createDepositActionContext(
                BehaviorActionType.BUY,
                11,
                500_000L,
                2_500_000L
        );
        List<BehaviorAnalysisResult> analysisResults = List.of(
                createAnalysisResult(),
                createAnalysisResult(createRule(
                        BehaviorRuleCode.DEPOSIT_CANCEL_AND_SECURITY_BUY,
                        5,
                        -10,
                        5
                ))
        );

        List<BehaviorAnalysisResult> adjustedResults = GameRuleEvaluationCondition
                .EXCLUSIVE_RATIO_AXIS_AND_DEPOSIT_DECISION
                .adjustDepositDecisionResults(
                        List.of(cancelContext, buyContext),
                        analysisResults
                );

        assertEquals(1, countDepositDecisionRules(adjustedResults));
        assertTotalScore(adjustedResults, "5", "-10", "5");
    }

    @Test
    @DisplayName("예금 해지 후 2 Tick 동안 현금 80% 이상을 유지하면 현금 유지 규칙만 적용한다.")
    void applyDepositCashRetentionRuleExclusively() {
        BehaviorContext cancelContext = createDepositActionContext(
                BehaviorActionType.CANCEL_PRODUCT,
                10,
                1_000_000L,
                3_000_000L
        );
        BehaviorContext smallBuyContext = createDepositActionContext(
                BehaviorActionType.BUY,
                12,
                400_000L,
                2_600_000L
        );

        List<BehaviorAnalysisResult> adjustedResults = GameRuleEvaluationCondition
                .EXCLUSIVE_RATIO_AXIS_AND_DEPOSIT_DECISION
                .adjustDepositDecisionResults(
                        List.of(cancelContext, smallBuyContext),
                        List.of(createAnalysisResult(), createAnalysisResult())
                );

        assertEquals(1, countDepositDecisionRules(adjustedResults));
        assertTotalScore(adjustedResults, "-5", "10", "-5");
    }

    @Test
    @DisplayName("예금 해지액 매수와 현금 유지 기준을 모두 충족하지 못하면 점수를 적용하지 않는다.")
    void skipAmbiguousDepositDecisionRule() {
        BehaviorContext cancelContext = createDepositActionContext(
                BehaviorActionType.CANCEL_PRODUCT,
                10,
                1_000_000L,
                3_000_000L
        );
        BehaviorContext smallBuyContext = createDepositActionContext(
                BehaviorActionType.BUY,
                12,
                400_000L,
                2_000_000L
        );

        List<BehaviorAnalysisResult> adjustedResults = GameRuleEvaluationCondition
                .EXCLUSIVE_RATIO_AXIS_AND_DEPOSIT_DECISION
                .adjustDepositDecisionResults(
                        List.of(cancelContext, smallBuyContext),
                        List.of(createAnalysisResult(), createAnalysisResult())
                );

        assertEquals(0, countDepositDecisionRules(adjustedResults));
        assertTotalScore(adjustedResults, "0", "0", "0");
    }

    @Test
    @DisplayName("급락장 매수는 매수 비율에 따라 5·10·15점의 정수 점수를 적용한다.")
    void applyFixedCrashBuyScoresByActionRatio() {
        assertFixedBuyScore(createBuyContext(900_000L, 10_000_000L),
                BehaviorRuleCode.CRASH_BUY, "5", "0", "0");
        assertFixedBuyScore(createBuyContext(1_000_000L, 10_000_000L),
                BehaviorRuleCode.CRASH_BUY, "10", "-5", "5");
        assertFixedBuyScore(createBuyContext(3_000_000L, 10_000_000L),
                BehaviorRuleCode.CRASH_BUY, "15", "-10", "5");
    }

    @Test
    @DisplayName("급등장 매수는 매수 비율이 커질수록 RT·LH·RP 강도가 증가한다.")
    void applyFixedBullBuyScoresByActionRatio() {
        assertFixedBuyScore(createBuyContext(900_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY, "5", "0", "5");
        assertFixedBuyScore(createBuyContext(1_000_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY, "5", "-5", "10");
        assertFixedBuyScore(createBuyContext(3_000_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY, "10", "-10", "15");
    }

    @Test
    @DisplayName("물타기 매수는 매수 비율에 따라 RT·LH만 정수 점수로 반영한다.")
    void applyFixedLossAveragingScoresByActionRatio() {
        assertFixedBuyScore(createBuyContext(900_000L, 10_000_000L),
                BehaviorRuleCode.LOSS_AVERAGING_BUY, "5", "0", "0");
        assertFixedBuyScore(createBuyContext(1_000_000L, 10_000_000L),
                BehaviorRuleCode.LOSS_AVERAGING_BUY, "10", "-5", "0");
        assertFixedBuyScore(createBuyContext(3_000_000L, 10_000_000L),
                BehaviorRuleCode.LOSS_AVERAGING_BUY, "15", "-10", "0");
    }

    @Test
    @DisplayName("손절 매도는 전량 매도 점수가 일부 매도보다 작아지지 않는다.")
    void applyMonotonicFixedLossCutScoresBySellRatio() {
        assertFixedSellScore(19, 81, false,
                BehaviorRuleCode.LOSS_CUT_SELL, "-5", "5", "0");
        assertFixedSellScore(20, 80, false,
                BehaviorRuleCode.LOSS_CUT_SELL, "-10", "10", "-5");
        assertFixedSellScore(50, 50, false,
                BehaviorRuleCode.LOSS_CUT_SELL, "-15", "15", "-10");
        assertFixedSellScore(100, 0, true,
                BehaviorRuleCode.LOSS_CUT_SELL, "-15", "15", "-10");
    }

    @Test
    @DisplayName("급등장 익절 매도는 전량 매도 점수가 일부 매도보다 작아지지 않는다.")
    void applyMonotonicFixedBullProfitScoresBySellRatio() {
        assertFixedSellScore(19, 81, false,
                BehaviorRuleCode.BULL_PROFIT_SELL, "0", "5", "0");
        assertFixedSellScore(20, 80, false,
                BehaviorRuleCode.BULL_PROFIT_SELL, "0", "10", "5");
        assertFixedSellScore(50, 50, false,
                BehaviorRuleCode.BULL_PROFIT_SELL, "0", "15", "10");
        assertFixedSellScore(100, 0, true,
                BehaviorRuleCode.BULL_PROFIT_SELL, "0", "15", "10");
    }

    @Test
    @DisplayName("급락장 매도는 20%·50%·전량 구간에 정수 점수를 적용한다.")
    void applyFixedCrashSellScoresBySellRatio() {
        assertFixedCrashSellScore(20, 80, false, "-5", "5", "0");
        assertFixedCrashSellScore(50, 50, false, "-10", "5", "-5");
        assertFixedCrashSellScore(100, 0, true, "-15", "10", "-5");
    }

    @Test
    @DisplayName("중간 정수안의 급락장 매수는 RT 중심으로 점수를 적용한다.")
    void applyModerateCrashBuyScoresByActionRatio() {
        assertModerateBuyScore(createBuyContext(900_000L, 10_000_000L),
                BehaviorRuleCode.CRASH_BUY, "5", "0", "0");
        assertModerateBuyScore(createBuyContext(1_000_000L, 10_000_000L),
                BehaviorRuleCode.CRASH_BUY, "10", "-5", "0");
        assertModerateBuyScore(createBuyContext(3_000_000L, 10_000_000L),
                BehaviorRuleCode.CRASH_BUY, "15", "-10", "0");
    }

    @Test
    @DisplayName("중간 정수안의 급등장 매수는 대규모 RP를 10점으로 제한한다.")
    void applyModerateBullBuyScoresByActionRatio() {
        assertModerateBuyScore(createBuyContext(900_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY, "5", "0", "5");
        assertModerateBuyScore(createBuyContext(1_000_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY, "5", "-5", "10");
        assertModerateBuyScore(createBuyContext(3_000_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY, "10", "-10", "10");
    }

    @Test
    @DisplayName("급등장 매수 RP 중심안은 RT와 RP 역할을 분리한다.")
    void applyRpCenteredBullBuyScoresByActionRatio() {
        assertBuyScore(
                GameRuleEvaluationCondition
                        .EXCLUSIVE_MODERATE_RP_CENTERED_BULL_BUY_AND_DEPOSIT_DECISION,
                createBuyContext(900_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY,
                "0", "0", "5"
        );
        assertBuyScore(
                GameRuleEvaluationCondition
                        .EXCLUSIVE_MODERATE_RP_CENTERED_BULL_BUY_AND_DEPOSIT_DECISION,
                createBuyContext(1_000_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY,
                "0", "-5", "10"
        );
        assertBuyScore(
                GameRuleEvaluationCondition
                        .EXCLUSIVE_MODERATE_RP_CENTERED_BULL_BUY_AND_DEPOSIT_DECISION,
                createBuyContext(3_000_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY,
                "5", "-10", "10"
        );
    }

    @Test
    @DisplayName("급등장 매수 RP 완화안은 RT를 유지하고 RP를 5점으로 제한한다.")
    void applyReducedRpBullBuyScoresByActionRatio() {
        GameRuleEvaluationCondition condition = GameRuleEvaluationCondition
                .EXCLUSIVE_MODERATE_REDUCED_BULL_BUY_RP_AND_DEPOSIT_DECISION;
        assertBuyScore(condition, createBuyContext(900_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY, "5", "0", "5");
        assertBuyScore(condition, createBuyContext(1_000_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY, "5", "-5", "5");
        assertBuyScore(condition, createBuyContext(3_000_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY, "10", "-10", "5");
    }

    @Test
    @DisplayName("급등장 매수 규모별 분리안은 중간 매수는 RP, 대규모 매수는 RT를 반영한다.")
    void applySizeSeparatedBullBuyScoresByActionRatio() {
        GameRuleEvaluationCondition condition = GameRuleEvaluationCondition
                .EXCLUSIVE_MODERATE_SIZE_SEPARATED_BULL_BUY_AND_DEPOSIT_DECISION;
        assertBuyScore(condition, createBuyContext(900_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY, "0", "0", "5");
        assertBuyScore(condition, createBuyContext(1_000_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY, "0", "-5", "10");
        assertBuyScore(condition, createBuyContext(3_000_000L, 10_000_000L),
                BehaviorRuleCode.BULL_BUY, "10", "-10", "5");
    }

    @Test
    @DisplayName("소규모 거래 제외 조건은 10% 미만 매수와 20% 미만 매도의 점수를 제거한다.")
    void excludeSmallTradeScores() {
        GameRuleEvaluationCondition condition = GameRuleEvaluationCondition
                .EXCLUSIVE_MODERATE_SIZE_SEPARATED_BULL_BUY_WITH_SMALL_TRADE_DEAD_ZONE;

        BehaviorAnalysisResult smallBuyResult = condition.excludeSmallTradeScores(
                createBuyContext(900_000L, 10_000_000L),
                createAnalysisResult(createRule(BehaviorRuleCode.CRASH_BUY, 5, 0, 0))
        );
        BehaviorAnalysisResult boundaryBuyResult = condition.excludeSmallTradeScores(
                createBuyContext(1_000_000L, 10_000_000L),
                createAnalysisResult(createRule(BehaviorRuleCode.CRASH_BUY, 10, -5, 0))
        );
        BehaviorAnalysisResult smallSellResult = condition.excludeSmallTradeScores(
                createSellContext(19, 81, false),
                createAnalysisResult(createRule(BehaviorRuleCode.LOSS_CUT_SELL, -5, 5, 0))
        );
        BehaviorAnalysisResult boundarySellResult = condition.excludeSmallTradeScores(
                createSellContext(20, 80, false),
                createAnalysisResult(createRule(BehaviorRuleCode.LOSS_CUT_SELL, -5, 5, -5))
        );

        assertTrue(smallBuyResult.getAppliedRules().isEmpty());
        assertFalse(boundaryBuyResult.getAppliedRules().isEmpty());
        assertTrue(smallSellResult.getAppliedRules().isEmpty());
        assertFalse(boundarySellResult.getAppliedRules().isEmpty());
    }

    @Test
    @DisplayName("중간 정수안의 손절 매도는 LH를 최대 10점으로 제한한다.")
    void applyModerateLossCutScoresBySellRatio() {
        assertModerateSellScore(19, 81, false,
                BehaviorRuleCode.LOSS_CUT_SELL, "-5", "5", "0");
        assertModerateSellScore(20, 80, false,
                BehaviorRuleCode.LOSS_CUT_SELL, "-5", "5", "-5");
        assertModerateSellScore(50, 50, false,
                BehaviorRuleCode.LOSS_CUT_SELL, "-10", "10", "-5");
        assertModerateSellScore(100, 0, true,
                BehaviorRuleCode.LOSS_CUT_SELL, "-15", "10", "-10");
    }

    @Test
    @DisplayName("중간 정수안의 급등장 익절은 LH를 최대 10점으로 제한한다.")
    void applyModerateBullProfitScoresBySellRatio() {
        assertModerateSellScore(19, 81, false,
                BehaviorRuleCode.BULL_PROFIT_SELL, "0", "5", "0");
        assertModerateSellScore(20, 80, false,
                BehaviorRuleCode.BULL_PROFIT_SELL, "0", "5", "5");
        assertModerateSellScore(50, 50, false,
                BehaviorRuleCode.BULL_PROFIT_SELL, "0", "10", "0");
        assertModerateSellScore(100, 0, true,
                BehaviorRuleCode.BULL_PROFIT_SELL, "0", "10", "0");
    }

    private void assertFixedBuyScore(
            BehaviorContext behaviorContext,
            BehaviorRuleCode ruleCode,
            String expectedRtDelta,
            String expectedLhDelta,
            String expectedRpDelta) {
        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_FIXED_ACTION_SCORE_AND_DEPOSIT_DECISION
                .adjustAnalysisResult(
                        behaviorContext,
                        createAnalysisResult(createRule(ruleCode, 1, 1, 1))
                );

        assertScore(adjustedResult, expectedRtDelta, expectedLhDelta, expectedRpDelta);
    }

    private void assertModerateBuyScore(
            BehaviorContext behaviorContext,
            BehaviorRuleCode ruleCode,
            String expectedRtDelta,
            String expectedLhDelta,
            String expectedRpDelta) {
        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_MODERATE_FIXED_ACTION_SCORE_AND_DEPOSIT_DECISION
                .adjustAnalysisResult(
                        behaviorContext,
                        createAnalysisResult(createRule(ruleCode, 1, 1, 1))
                );

        assertScore(adjustedResult, expectedRtDelta, expectedLhDelta, expectedRpDelta);
    }

    private void assertBuyScore(
            GameRuleEvaluationCondition condition,
            BehaviorContext behaviorContext,
            BehaviorRuleCode ruleCode,
            String expectedRtDelta,
            String expectedLhDelta,
            String expectedRpDelta) {
        BehaviorAnalysisResult adjustedResult = condition.adjustAnalysisResult(
                behaviorContext,
                createAnalysisResult(createRule(ruleCode, 1, 1, 1))
        );

        assertScore(adjustedResult, expectedRtDelta, expectedLhDelta, expectedRpDelta);
    }

    private void assertFixedSellScore(
            int sellQuantity,
            int remainingQuantity,
            boolean fullSecuritySell,
            BehaviorRuleCode ruleCode,
            String expectedRtDelta,
            String expectedLhDelta,
            String expectedRpDelta) {
        BehaviorContext behaviorContext = createSellContext(
                sellQuantity,
                remainingQuantity,
                fullSecuritySell
        );
        behaviorContext.setMarketState(ruleCode == BehaviorRuleCode.BULL_PROFIT_SELL
                ? MarketState.BULL
                : MarketState.NORMAL);
        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_FIXED_ACTION_SCORE_AND_DEPOSIT_DECISION
                .adjustAnalysisResult(
                        behaviorContext,
                        createAnalysisResult(createRule(ruleCode, 1, 1, 1))
                );

        assertScore(adjustedResult, expectedRtDelta, expectedLhDelta, expectedRpDelta);
    }

    private void assertFixedCrashSellScore(
            int sellQuantity,
            int remainingQuantity,
            boolean fullSecuritySell,
            String expectedRtDelta,
            String expectedLhDelta,
            String expectedRpDelta) {
        BehaviorContext behaviorContext = createSellContext(
                sellQuantity,
                remainingQuantity,
                fullSecuritySell
        );
        BehaviorAnalysisResult analysisResult = fullSecuritySell
                ? createAnalysisResult(createRule(BehaviorRuleCode.CRASH_FULL_SELL, 1, 1, 1))
                : createAnalysisResult();
        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_FIXED_ACTION_SCORE_AND_DEPOSIT_DECISION
                .adjustAnalysisResult(behaviorContext, analysisResult);

        assertScore(adjustedResult, expectedRtDelta, expectedLhDelta, expectedRpDelta);
    }

    private void assertModerateSellScore(
            int sellQuantity,
            int remainingQuantity,
            boolean fullSecuritySell,
            BehaviorRuleCode ruleCode,
            String expectedRtDelta,
            String expectedLhDelta,
            String expectedRpDelta) {
        BehaviorContext behaviorContext = createSellContext(
                sellQuantity,
                remainingQuantity,
                fullSecuritySell
        );
        behaviorContext.setMarketState(ruleCode == BehaviorRuleCode.BULL_PROFIT_SELL
                ? MarketState.BULL
                : MarketState.NORMAL);
        BehaviorAnalysisResult adjustedResult = GameRuleEvaluationCondition
                .EXCLUSIVE_MODERATE_FIXED_ACTION_SCORE_AND_DEPOSIT_DECISION
                .adjustAnalysisResult(
                        behaviorContext,
                        createAnalysisResult(createRule(ruleCode, 1, 1, 1))
                );

        assertScore(adjustedResult, expectedRtDelta, expectedLhDelta, expectedRpDelta);
    }

    private BehaviorContext createBuyContext(long actionAmount, long totalAssetPrincipal) {
        BehaviorEvent behaviorEvent = new BehaviorEvent();
        behaviorEvent.setActionType(BehaviorActionType.BUY);
        behaviorEvent.setAssetType(BehaviorAssetType.SECURITY);
        behaviorEvent.setActionAmount(actionAmount);
        behaviorEvent.setCurrentCash(totalAssetPrincipal - actionAmount);
        behaviorEvent.setCurrentStockPrincipal(actionAmount);
        behaviorEvent.setCurrentDeposit(0L);

        BehaviorContext behaviorContext = new BehaviorContext();
        behaviorContext.setCurrentEvent(behaviorEvent);
        behaviorContext.setMarketState(MarketState.CRASH);
        return behaviorContext;
    }

    private BehaviorContext createSellContext(
            int sellQuantity,
            int remainingQuantity,
            boolean fullSecuritySell) {
        BehaviorEvent behaviorEvent = new BehaviorEvent();
        behaviorEvent.setActionType(BehaviorActionType.SELL);
        behaviorEvent.setAssetType(BehaviorAssetType.SECURITY);
        behaviorEvent.setQuantity(sellQuantity);
        behaviorEvent.setCurrentSecurityQuantity(remainingQuantity);
        behaviorEvent.setActionAmount(1_000_000L);
        behaviorEvent.setCurrentCash(3_000_000L);
        behaviorEvent.setCurrentStockPrincipal(7_000_000L);
        behaviorEvent.setCurrentDeposit(0L);

        BehaviorContext behaviorContext = new BehaviorContext();
        behaviorContext.setCurrentEvent(behaviorEvent);
        behaviorContext.setMarketState(MarketState.CRASH);
        behaviorContext.setFullSecuritySell(fullSecuritySell);
        return behaviorContext;
    }

    private BehaviorContext createDepositActionContext(
            BehaviorActionType actionType,
            int gameTick,
            long actionAmount,
            long currentCash) {
        BehaviorEvent behaviorEvent = new BehaviorEvent();
        behaviorEvent.setActionType(actionType);
        behaviorEvent.setAssetType(actionType == BehaviorActionType.BUY
                ? BehaviorAssetType.SECURITY
                : BehaviorAssetType.PRODUCT);
        behaviorEvent.setGameTick(gameTick);
        behaviorEvent.setActionAmount(actionAmount);
        behaviorEvent.setCurrentCash(currentCash);
        behaviorEvent.setCurrentStockPrincipal(7_000_000L);
        behaviorEvent.setCurrentDeposit(0L);

        BehaviorContext behaviorContext = new BehaviorContext();
        behaviorContext.setCurrentEvent(behaviorEvent);
        behaviorContext.setMarketState(MarketState.NORMAL);
        return behaviorContext;
    }

    private BehaviorAnalysisResult createAnalysisResult(RuleResult... ruleResults) {
        return new BehaviorAnalysisResult(List.of(ruleResults));
    }

    private RuleResult createRule(
            BehaviorRuleCode ruleCode,
            int rtDelta,
            int lhDelta,
            int rpDelta) {
        return new RuleResult(
                ruleCode,
                ScoreDelta.createScoreDelta(rtDelta, lhDelta, rpDelta),
                ruleCode.name()
        );
    }

    private boolean containsRule(
            BehaviorAnalysisResult analysisResult,
            BehaviorRuleCode ruleCode) {
        return analysisResult.getAppliedRules().stream()
                .anyMatch(ruleResult -> ruleResult.getRuleCode() == ruleCode);
    }

    private void assertScore(
            BehaviorAnalysisResult analysisResult,
            String expectedRtDelta,
            String expectedLhDelta,
            String expectedRpDelta) {
        assertEquals(
                0,
                new BigDecimal(expectedRtDelta).compareTo(
                        analysisResult.getTotalScoreDelta().getRtDelta()
                )
        );
        assertEquals(
                0,
                new BigDecimal(expectedLhDelta).compareTo(
                        analysisResult.getTotalScoreDelta().getLhDelta()
                )
        );
        assertEquals(
                0,
                new BigDecimal(expectedRpDelta).compareTo(
                        analysisResult.getTotalScoreDelta().getRpDelta()
                )
        );
    }

    private int countDepositDecisionRules(List<BehaviorAnalysisResult> analysisResults) {
        return (int) analysisResults.stream()
                .map(BehaviorAnalysisResult::getAppliedRules)
                .flatMap(List::stream)
                .filter(ruleResult -> ruleResult.getRuleCode()
                        == BehaviorRuleCode.DEPOSIT_CANCEL_AND_SECURITY_BUY)
                .count();
    }

    private void assertTotalScore(
            List<BehaviorAnalysisResult> analysisResults,
            String expectedRtDelta,
            String expectedLhDelta,
            String expectedRpDelta) {
        ScoreDelta totalScoreDelta = analysisResults.stream()
                .map(BehaviorAnalysisResult::getTotalScoreDelta)
                .reduce(ScoreDelta.createZeroScoreDelta(), ScoreDelta::addScoreDelta);
        assertEquals(0, new BigDecimal(expectedRtDelta).compareTo(totalScoreDelta.getRtDelta()));
        assertEquals(0, new BigDecimal(expectedLhDelta).compareTo(totalScoreDelta.getLhDelta()));
        assertEquals(0, new BigDecimal(expectedRpDelta).compareTo(totalScoreDelta.getRpDelta()));
    }
}
