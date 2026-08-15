package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kkobi.game.service.ScenarioService;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@EnabledIfEnvironmentVariable(
        named = "ASSESSMENT_SMALL_TRADE_DEAD_ZONE_ENABLED",
        matches = "true"
)
class SmallTradeDeadZoneFrequencyComparisonCsvExecutionTest {

    private static final int DEFAULT_SIMULATION_COUNT = 10_000;
    private static final long DEFAULT_RANDOM_SEED = 20260826L;
    private static final DateTimeFormatter OUTPUT_DIRECTORY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Test
    @DisplayName("소규모 거래 제외 전후의 저·중·고빈도별 성향 분포를 비교한다.")
    void exportSmallTradeDeadZoneDistributionByFrequency() {
        int simulationCount = getSimulationCount();
        long randomSeed = getRandomSeed();
        Path outputDirectory = Path.of(
                "build",
                "assessment-simulation",
                "small-trade-dead-zone-"
                        + LocalDateTime.now().format(OUTPUT_DIRECTORY_FORMATTER)
        );

        new SmallTradeDeadZoneFrequencyComparisonCsvExporter().exportComparison(
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
        String simulationCount = System.getenv("ASSESSMENT_SMALL_TRADE_DEAD_ZONE_COUNT");
        return simulationCount == null
                ? DEFAULT_SIMULATION_COUNT
                : Integer.parseInt(simulationCount);
    }

    private long getRandomSeed() {
        String randomSeed = System.getenv("ASSESSMENT_SMALL_TRADE_DEAD_ZONE_SEED");
        return randomSeed == null
                ? DEFAULT_RANDOM_SEED
                : Long.parseLong(randomSeed);
    }
}
