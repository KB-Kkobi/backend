package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kkobi.game.service.ScenarioService;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@EnabledIfEnvironmentVariable(
        named = "ASSESSMENT_FREQUENCY_STABILITY_ENABLED",
        matches = "true"
)
class GameBehaviorFrequencyStabilityCsvExecutionTest {

    private static final int DEFAULT_SIMULATION_COUNT_PER_CONDITION = 10_000;
    private static final String DEFAULT_RANDOM_SEEDS =
            "20260815,20260816,20260817,20260818,20260819";

    @Test
    @DisplayName("5개 Seed의 행동 빈도별 성향 분포 안정성을 검증한다.")
    void exportFrequencyStabilityCsv() {
        int simulationCount = getSimulationCount();
        List<Long> randomSeeds = getRandomSeeds();
        Path outputDirectory = Path.of(
                "build",
                "assessment-simulation",
                "frequency-stability"
        );

        new GameBehaviorFrequencyStabilityCsvExporter().exportFrequencyStability(
                new ScenarioService().getScenario("SC001"),
                simulationCount,
                randomSeeds,
                outputDirectory
        );

        System.out.println("simulation_count_per_condition=" + simulationCount);
        System.out.println("random_seeds=" + randomSeeds);
        System.out.println("output_directory=" + outputDirectory.toAbsolutePath());
    }

    private int getSimulationCount() {
        String simulationCount = System.getenv("ASSESSMENT_FREQUENCY_STABILITY_COUNT");
        return simulationCount == null
                ? DEFAULT_SIMULATION_COUNT_PER_CONDITION
                : Integer.parseInt(simulationCount);
    }

    private List<Long> getRandomSeeds() {
        String randomSeeds = System.getenv("ASSESSMENT_FREQUENCY_STABILITY_SEEDS");
        String seedValues = randomSeeds == null ? DEFAULT_RANDOM_SEEDS : randomSeeds;
        return Arrays.stream(seedValues.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();
    }
}
