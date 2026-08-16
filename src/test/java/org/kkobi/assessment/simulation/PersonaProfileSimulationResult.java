package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.PersonaType;

import java.util.Objects;

public record PersonaProfileSimulationResult(
        long randomSeed,
        PersonaType targetPersona,
        GameBehaviorSimulationResult simulationResult) {

    public PersonaProfileSimulationResult {
        Objects.requireNonNull(targetPersona, "목표 성향은 필수입니다.");
        Objects.requireNonNull(simulationResult, "시뮬레이션 결과는 필수입니다.");
    }

    public PersonaType predictedPersona() {
        return simulationResult.getPersonaType();
    }

    public boolean matchesTarget() {
        return targetPersona == predictedPersona();
    }
}
