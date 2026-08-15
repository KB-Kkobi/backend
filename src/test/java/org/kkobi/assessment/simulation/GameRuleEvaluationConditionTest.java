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
}
