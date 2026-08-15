package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kkobi.game.service.ScenarioService;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@EnabledIfEnvironmentVariable(
        named = "ASSESSMENT_GAME_STABILITY_ENABLED",
        matches = "true"
)
class GameBiasMitigationStabilityCsvExecutionTest {

    private static final int DEFAULT_SIMULATION_COUNT = 10_000;
    private static final List<Long> DEFAULT_RANDOM_SEEDS = List.of(
            20260826L,
            20260827L,
            20260828L,
            20260829L,
            20260830L
    );

    @Test
    @DisplayName("편중 완화 후보안의 다중 Seed 안정성과 축 상관관계를 분석한다.")
    void exportSeedStabilityAndAxisCorrelation() {
        int simulationCount = getSimulationCount();
        List<Long> randomSeeds = getRandomSeeds();
        Path outputDirectory = Path.of(
                "build",
                "assessment-simulation",
                "game-bias-stability"
        );

        new GameBiasMitigationStabilityCsvExporter().exportStability(
                new ScenarioService().getScenario("SC001"),
                simulationCount,
                randomSeeds,
                outputDirectory
        );

        System.out.println("simulation_count_per_seed=" + simulationCount);
        System.out.println("random_seeds=" + randomSeeds);
        System.out.println("output_directory=" + outputDirectory.toAbsolutePath());
    }

    private int getSimulationCount() {
        String simulationCount = System.getenv("ASSESSMENT_GAME_STABILITY_COUNT");
        return simulationCount == null
                ? DEFAULT_SIMULATION_COUNT
                : Integer.parseInt(simulationCount);
    }

    private List<Long> getRandomSeeds() {
        String randomSeeds = System.getenv("ASSESSMENT_GAME_STABILITY_SEEDS");
        if (randomSeeds == null || randomSeeds.isBlank()) {
            return DEFAULT_RANDOM_SEEDS;
        }
        return Arrays.stream(randomSeeds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();
    }
}
