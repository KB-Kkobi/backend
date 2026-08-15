package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kkobi.game.service.ScenarioService;

import java.nio.file.Path;

@EnabledIfEnvironmentVariable(
        named = "ASSESSMENT_RULE_ACCUMULATION_SIMULATION_ENABLED",
        matches = "true"
)
class RuleAccumulationComparisonCsvExecutionTest {

    private static final int DEFAULT_SIMULATION_COUNT_PER_CONDITION = 10_000;
    private static final long DEFAULT_RANDOM_SEED = 20260815L;

    @Test
    @DisplayName("규칙 누적 방식 3종을 빈도별 10,000명에게 실행한다.")
    void exportRuleAccumulationComparisonCsv() {
        int simulationCount = getSimulationCount();
        long randomSeed = getRandomSeed();
        Path outputDirectory = Path.of(
                "build",
                "assessment-simulation",
                "rule-accumulation-seed-" + randomSeed
        );

        new RuleAccumulationComparisonCsvExporter().exportRuleAccumulationComparison(
                new ScenarioService().getScenario("SC001"),
                simulationCount,
                randomSeed,
                outputDirectory
        );

        System.out.println("simulation_count_per_condition=" + simulationCount);
        System.out.println("random_seed=" + randomSeed);
        System.out.println("output_directory=" + outputDirectory.toAbsolutePath());
    }

    private int getSimulationCount() {
        String simulationCount = System.getenv("ASSESSMENT_RULE_ACCUMULATION_SIMULATION_COUNT");
        return simulationCount == null
                ? DEFAULT_SIMULATION_COUNT_PER_CONDITION
                : Integer.parseInt(simulationCount);
    }

    private long getRandomSeed() {
        String randomSeed = System.getenv("ASSESSMENT_RULE_ACCUMULATION_SIMULATION_SEED");
        return randomSeed == null
                ? DEFAULT_RANDOM_SEED
                : Long.parseLong(randomSeed);
    }
}
