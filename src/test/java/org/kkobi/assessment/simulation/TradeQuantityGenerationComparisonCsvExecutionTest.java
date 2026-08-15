package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kkobi.game.service.ScenarioService;

import java.nio.file.Path;

@EnabledIfEnvironmentVariable(
        named = "ASSESSMENT_TRADE_QUANTITY_SIMULATION_ENABLED",
        matches = "true"
)
class TradeQuantityGenerationComparisonCsvExecutionTest {

    private static final int DEFAULT_SIMULATION_COUNT = 10_000;
    private static final long DEFAULT_RANDOM_SEED = 20260825L;

    @Test
    @DisplayName("기존 수량과 매수·매도 대칭 수량을 10,000명에게서 비교한다.")
    void exportTradeQuantityGenerationComparison() {
        int simulationCount = getSimulationCount();
        long randomSeed = getRandomSeed();
        Path outputDirectory = Path.of(
                "build",
                "assessment-simulation",
                "trade-quantity-seed-" + randomSeed
        );

        new TradeQuantityGenerationComparisonCsvExporter().exportComparison(
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
        String simulationCount = System.getenv("ASSESSMENT_TRADE_QUANTITY_SIMULATION_COUNT");
        return simulationCount == null
                ? DEFAULT_SIMULATION_COUNT
                : Integer.parseInt(simulationCount);
    }

    private long getRandomSeed() {
        String randomSeed = System.getenv("ASSESSMENT_TRADE_QUANTITY_SIMULATION_SEED");
        return randomSeed == null
                ? DEFAULT_RANDOM_SEED
                : Long.parseLong(randomSeed);
    }
}
