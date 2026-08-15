package org.kkobi.assessment.simulation;

import lombok.Getter;
import org.kkobi.assessment.domain.RuleResult;
import org.kkobi.assessment.domain.ScoreDelta;
import org.kkobi.assessment.enums.BehaviorRuleCode;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public enum LossAveragingRtWeightCondition {

    RT_15("기존 물타기 RT +15", 15),
    RT_10("물타기 RT +10", 10),
    RT_5("물타기 RT +5", 5);

    private static final BigDecimal BASE_RT_WEIGHT = BigDecimal.valueOf(15);
    private static final int SCORE_SCALE = 2;

    private final String description;
    private final int rtWeight;

    LossAveragingRtWeightCondition(String description, int rtWeight) {
        this.description = description;
        this.rtWeight = rtWeight;
    }

    public RuleResult adjustRuleResult(RuleResult ruleResult) {
        if (ruleResult.getRuleCode() != BehaviorRuleCode.LOSS_AVERAGING_BUY) {
            return ruleResult;
        }

        ScoreDelta scoreDelta = ruleResult.getScoreDelta();
        BigDecimal adjustedRtDelta = scoreDelta.getRtDelta()
                .multiply(BigDecimal.valueOf(rtWeight))
                .divide(BASE_RT_WEIGHT, SCORE_SCALE, RoundingMode.HALF_UP);
        return new RuleResult(
                ruleResult.getRuleCode(),
                new ScoreDelta(
                        adjustedRtDelta,
                        scoreDelta.getLhDelta(),
                        scoreDelta.getRpDelta()
                ),
                ruleResult.getReason()
        );
    }
}
