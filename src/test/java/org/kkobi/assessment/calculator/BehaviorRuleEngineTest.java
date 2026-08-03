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

        BehaviorAnalysisResult result = behaviorRuleEngine.calculateBehaviorAnalysis(context);

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

        BehaviorAnalysisResult result = behaviorRuleEngine.calculateBehaviorAnalysis(context);

        assertEquals(2, result.getAppliedRules().size());
        assertScoreEquals("30.00", result.getTotalScoreDelta().getRtDelta());
        assertScoreEquals("-12.00", result.getTotalScoreDelta().getLhDelta());
        assertScoreEquals("12.00", result.getTotalScoreDelta().getRpDelta());
    }

    private void assertScoreEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
