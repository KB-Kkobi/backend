package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kkobi.game.service.ScenarioService;

import java.nio.file.Path;

@EnabledIfEnvironmentVariable(
        named = "ASSESSMENT_GAME_RULE_COMPARISON_ENABLED",
        matches = "true"
)
class GameRuleEvaluationComparisonCsvExecutionTest {

    private static final int DEFAULT_SIMULATION_COUNT = 10_000;
    private static final long DEFAULT_RANDOM_SEED = 20260815L;

    @Test
    @DisplayName("기존 규칙과 우선순위·거래 비율 대안을 중간 빈도 10,000명으로 비교한다.")
    void exportGameRuleEvaluationComparisonCsv() {
        int simulationCount = getSimulationCount();
        long randomSeed = getRandomSeed();
        Path outputDirectory = Path.of(
                "build",
                "assessment-simulation",
                "game-rule-comparison-seed-" + randomSeed
        );

        new GameRuleEvaluationComparisonCsvExporter().exportComparison(
                new ScenarioService().getScenario("SC001"),
                simulationCount,
                randomSeed,
                outputDirectory
        );

        System.out.println("simulation_count=" + simulationCount);
        System.out.println("random_seed=" + randomSeed);
        System.out.println("output_directory=" + outputDirectory.toAbsolutePath());
    }

    private int getSimulationCount() {
        String simulationCount = System.getenv("ASSESSMENT_GAME_RULE_COMPARISON_COUNT");
        return simulationCount == null
                ? DEFAULT_SIMULATION_COUNT
                : Integer.parseInt(simulationCount);
    }

    private long getRandomSeed() {
        String randomSeed = System.getenv("ASSESSMENT_GAME_RULE_COMPARISON_SEED");
        return randomSeed == null
                ? DEFAULT_RANDOM_SEED
                : Long.parseLong(randomSeed);
    }
}
