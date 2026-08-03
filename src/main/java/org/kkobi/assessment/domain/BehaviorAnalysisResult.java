package org.kkobi.assessment.domain;

import lombok.Getter;

import java.util.List;

@Getter
public class BehaviorAnalysisResult {

    private final List<RuleResult> appliedRules;
    private final ScoreDelta totalScoreDelta;

    public BehaviorAnalysisResult(List<RuleResult> appliedRules) {
        this.appliedRules = List.copyOf(appliedRules);
        this.totalScoreDelta = this.appliedRules.stream()
                .map(RuleResult::getScoreDelta)
                .reduce(ScoreDelta.createZeroScoreDelta(), ScoreDelta::addScoreDelta);
    }

    public boolean hasAppliedRules() {
        return !appliedRules.isEmpty();
    }
}
