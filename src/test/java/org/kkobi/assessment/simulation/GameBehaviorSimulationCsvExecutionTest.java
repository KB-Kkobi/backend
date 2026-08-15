package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.service.ScenarioService;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(
        named = "ASSESSMENT_SIMULATION_ENABLED",
        matches = "true"
)
class GameBehaviorSimulationCsvExecutionTest {

    private static final int DEFAULT_SIMULATION_COUNT = 10_000;
    private static final long DEFAULT_RANDOM_SEED = 20260815L;

    @Test
    @DisplayName("중립 사용자 시뮬레이션 결과를 CSV로 출력한다.")
    void exportNeutralGameSimulationCsv() {
        int simulationCount = getSimulationCount();
        long randomSeed = getRandomSeed();
        Path outputDirectory = Path.of(
                "build",
                "assessment-simulation",
                "seed-" + randomSeed
        );
        GameBehaviorSimulationAnalysis analysis = new GameBehaviorSimulationCsvExporter()
                .exportGameSimulations(
                        new ScenarioService().getScenario("SC001"),
                        simulationCount,
                        randomSeed,
                        outputDirectory
                );

        System.out.println("simulation_count=" + analysis.getTotalSimulationCount());
        System.out.println("random_seed=" + randomSeed);
        System.out.println("output_directory=" + outputDirectory.toAbsolutePath());
        for (PersonaType personaType : PersonaType.values()) {
            GameBehaviorSimulationAnalysis.PersonaSummary summary =
                    analysis.getPersonaSummary(personaType);
            System.out.println(
                    personaType + "=" + summary.getSimulationCount()
                            + " (" + summary.getDistributionRate() + "%)"
            );
        }

        assertEquals(simulationCount, analysis.getTotalSimulationCount());
    }

    private int getSimulationCount() {
        String simulationCount = System.getenv("ASSESSMENT_SIMULATION_COUNT");
        return simulationCount == null
                ? DEFAULT_SIMULATION_COUNT
                : Integer.parseInt(simulationCount);
    }

    private long getRandomSeed() {
        String randomSeed = System.getenv("ASSESSMENT_SIMULATION_SEED");
        return randomSeed == null
                ? DEFAULT_RANDOM_SEED
                : Long.parseLong(randomSeed);
    }
}
