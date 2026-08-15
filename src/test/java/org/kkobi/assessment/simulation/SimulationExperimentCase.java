package org.kkobi.assessment.simulation;

import java.util.Objects;

public record SimulationExperimentCase(
        String name,
        String description,
        GameBiasMitigationCondition mitigationCondition,
        GameBehaviorFrequencyCondition frequencyCondition) {

    public SimulationExperimentCase {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("실험 이름은 필수입니다.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("실험 설명은 필수입니다.");
        }
        Objects.requireNonNull(mitigationCondition, "편중 완화 조건은 필수입니다.");
        Objects.requireNonNull(frequencyCondition, "행동 빈도 조건은 필수입니다.");
    }
}
