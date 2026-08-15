package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kkobi.game.service.ScenarioService;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@EnabledIfEnvironmentVariable(
        named = "ASSESSMENT_GAME_BIAS_FREQUENCY_ENABLED",
        matches = "true"
)
class GameBiasMitigationFrequencyComparisonCsvExecutionTest {

    private static final int DEFAULT_SIMULATION_COUNT = 10_000;
    private static final long DEFAULT_RANDOM_SEED = 20260826L;
    private static final DateTimeFormatter OUTPUT_DIRECTORY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Test
    @DisplayName("현재 편중 완화 규칙의 저·중·고빈도별 성향 분포를 비교한다.")
    void exportCurrentMitigationRuleDistributionByFrequency() {
        int simulationCount = getSimulationCount();
        long randomSeed = getRandomSeed();
        Path outputDirectory = Path.of(
                "build",
                "assessment-simulation",
                "game-bias-frequency-"
                        + LocalDateTime.now().format(OUTPUT_DIRECTORY_FORMATTER)
        );

        new GameBiasMitigationFrequencyComparisonCsvExporter().exportComparison(
                new ScenarioService().getScenario("SC001"),
                simulationCount,
                randomSeed,
                outputDirectory
        );

        System.out.println("simulation_count_per_frequency=" + simulationCount);
        System.out.println("random_seed=" + randomSeed);
        System.out.println("output_directory=" + outputDirectory.toAbsolutePath());
    }

    private int getSimulationCount() {
        String simulationCount = System.getenv("ASSESSMENT_GAME_BIAS_FREQUENCY_COUNT");
        return simulationCount == null
                ? DEFAULT_SIMULATION_COUNT
                : Integer.parseInt(simulationCount);
    }

    private long getRandomSeed() {
        String randomSeed = System.getenv("ASSESSMENT_GAME_BIAS_FREQUENCY_SEED");
        return randomSeed == null
                ? DEFAULT_RANDOM_SEED
                : Long.parseLong(randomSeed);
    }
}
