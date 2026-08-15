package org.kkobi.assessment.simulation;

import lombok.Getter;

@Getter
public enum SameTickRuleApplicationCondition {

    REPEATED("기존 방식", "같은 Tick에서 동일 규칙 반복 적용"),
    ONCE_PER_TICK("Tick당 1회", "같은 Tick에서 동일 규칙 최초 1회만 적용");

    private final String description;
    private final String criteria;

    SameTickRuleApplicationCondition(String description, String criteria) {
        this.description = description;
        this.criteria = criteria;
    }
}
