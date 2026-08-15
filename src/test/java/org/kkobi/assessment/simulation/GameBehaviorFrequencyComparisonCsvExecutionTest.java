package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.service.ScenarioService;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(
        named = "ASSESSMENT_FREQUENCY_SIMULATION_ENABLED",
        matches = "true"
)
class GameBehaviorFrequencyComparisonCsvExecutionTest {

    private static final int DEFAULT_SIMULATION_COUNT_PER_CONDITION = 10_000;
    private static final long DEFAULT_RANDOM_SEED = 20260815L;

    @Test
    @DisplayName("행동 빈도 조건별 10,000명 성향 분포를 CSV로 출력한다.")
    void exportFrequencyComparisonCsv() {
        int simulationCount = getSimulationCount();
        long randomSeed = getRandomSeed();
        Path outputDirectory = Path.of(
                "build",
                "assessment-simulation",
                "frequency-seed-" + randomSeed
        );

        Map<GameBehaviorFrequencyCondition, GameBehaviorSimulationAnalysis> analyses =
                new GameBehaviorFrequencyComparisonCsvExporter().exportFrequencyComparison(
                        new ScenarioService().getScenario("SC001"),
                        simulationCount,
                        randomSeed,
                        outputDirectory
                );

        System.out.println("simulation_count_per_condition=" + simulationCount);
        System.out.println("random_seed=" + randomSeed);
        System.out.println("output_directory=" + outputDirectory.toAbsolutePath());
        for (GameBehaviorFrequencyCondition condition
                : GameBehaviorFrequencyCondition.values()) {
            GameBehaviorSimulationAnalysis analysis = analyses.get(condition);
            System.out.println("[" + condition.getDescription() + "]");
            for (PersonaType personaType : PersonaType.values()) {
                System.out.println(
                        personaType + "="
                                + analysis.getPersonaSummary(personaType).getDistributionRate()
                                + "%"
                );
            }
        }

        assertEquals(GameBehaviorFrequencyCondition.values().length, analyses.size());
    }

    private int getSimulationCount() {
        String simulationCount = System.getenv("ASSESSMENT_FREQUENCY_SIMULATION_COUNT");
        return simulationCount == null
                ? DEFAULT_SIMULATION_COUNT_PER_CONDITION
                : Integer.parseInt(simulationCount);
    }

    private long getRandomSeed() {
        String randomSeed = System.getenv("ASSESSMENT_FREQUENCY_SIMULATION_SEED");
        return randomSeed == null
                ? DEFAULT_RANDOM_SEED
                : Long.parseLong(randomSeed);
    }
}
