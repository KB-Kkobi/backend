package org.kkobi.assessment.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.enums.BehaviorRuleCode;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class RuleResult {

    private final BehaviorRuleCode ruleCode;
    private final ScoreDelta scoreDelta;
    private final String reason;

    public RuleResult multiplyScoreDelta(BigDecimal multiplier) {
        return new RuleResult(ruleCode, scoreDelta.multiplyScoreDelta(multiplier), reason);
    }
}
