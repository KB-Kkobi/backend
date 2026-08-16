package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.service.ScenarioService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonaProfileSimulationAnalysisTest {

    @Test
    @DisplayName("동일 Seed의 유형별 시뮬레이션 결과는 재현된다.")
    void reproduceResultsWithSameSeed() {
        PersonaProfileSimulationRunner runner = new PersonaProfileSimulationRunner();

        List<PersonaProfileSimulationResult> first = runner.run(
                new ScenarioService().getScenario("SC001"), 2, 20260816L);
        List<PersonaProfileSimulationResult> second = runner.run(
                new ScenarioService().getScenario("SC001"), 2, 20260816L);

        assertEquals(
                first.stream().map(PersonaProfileSimulationResult::predictedPersona).toList(),
                second.stream().map(PersonaProfileSimulationResult::predictedPersona).toList()
        );
        assertEquals(PersonaType.values().length * 2, first.size());
    }
}
