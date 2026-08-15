package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.RuleResult;
import org.kkobi.assessment.domain.ScoreDelta;
import org.kkobi.assessment.enums.BehaviorRuleCode;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LossAveragingRtWeightConditionTest {

    @Test
    @DisplayName("물타기 RT만 15에서 10과 5로 조정한다.")
    void adjustLossAveragingRtWeight() {
        RuleResult ruleResult = createRuleResult(
                BehaviorRuleCode.LOSS_AVERAGING_BUY,
                "22.50",
                "-7.50",
                "7.50"
        );

        RuleResult rtTenResult = LossAveragingRtWeightCondition.RT_10
                .adjustRuleResult(ruleResult);
        RuleResult rtFiveResult = LossAveragingRtWeightCondition.RT_5
                .adjustRuleResult(ruleResult);

        assertEquals(new BigDecimal("15.00"), rtTenResult.getScoreDelta().getRtDelta());
        assertEquals(new BigDecimal("7.50"), rtFiveResult.getScoreDelta().getRtDelta());
        assertEquals(new BigDecimal("-7.50"), rtTenResult.getScoreDelta().getLhDelta());
        assertEquals(new BigDecimal("7.50"), rtFiveResult.getScoreDelta().getRpDelta());
    }

    @Test
    @DisplayName("물타기 외 규칙은 변경하지 않는다.")
    void preserveOtherRule() {
        RuleResult ruleResult = createRuleResult(
                BehaviorRuleCode.CRASH_BUY,
                "10",
                "-5",
                "5"
        );

        RuleResult adjustedResult = LossAveragingRtWeightCondition.RT_5
                .adjustRuleResult(ruleResult);

        assertSame(ruleResult, adjustedResult);
    }

    private RuleResult createRuleResult(
            BehaviorRuleCode ruleCode,
            String rtDelta,
            String lhDelta,
            String rpDelta) {
        return new RuleResult(
                ruleCode,
                new ScoreDelta(
                        new BigDecimal(rtDelta),
                        new BigDecimal(lhDelta),
                        new BigDecimal(rpDelta)
                ),
                "테스트 규칙"
        );
    }
}
