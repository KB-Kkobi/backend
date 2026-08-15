package org.kkobi.assessment.simulation;

import lombok.Getter;

@Getter
public enum ConsecutiveActionMultiplierCondition {

    ENABLED("기존 배율 적용", "2회 1.2배, 3회 이상 1.5배"),
    DISABLED("연속 배율 미적용", "모든 행동 1.0배");

    private final String description;
    private final String criteria;

    ConsecutiveActionMultiplierCondition(String description, String criteria) {
        this.description = description;
        this.criteria = criteria;
    }
}
