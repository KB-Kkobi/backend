package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kkobi.game.service.ScenarioService;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@EnabledIfEnvironmentVariable(
        named = "ASSESSMENT_SIMULATION_EXPERIMENT_ENABLED",
        matches = "true"
)
class SimulationComparisonCsvExecutionTest {

    private static final int DEFAULT_SIMULATION_COUNT = 10_000;
    private static final long DEFAULT_RANDOM_SEED = 20260826L;
    private static final DateTimeFormatter OUTPUT_DIRECTORY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Test
    @DisplayName("등록된 성향 시뮬레이션 비교 실험을 실행한다.")
    void exportConfiguredExperiment() {
        SimulationExperimentCatalog experimentCatalog = getExperimentCatalog();
        int simulationCount = getSimulationCount();
        long randomSeed = getRandomSeed();
        Path outputDirectory = Path.of(
                "build",
                "assessment-simulation",
                experimentCatalog.name().toLowerCase() + "-"
                        + LocalDateTime.now().format(OUTPUT_DIRECTORY_FORMATTER)
        );

        new SimulationComparisonCsvExporter().exportComparison(
                new ScenarioService().getScenario("SC001"),
                experimentCatalog.getExperimentCases(),
                simulationCount,
                randomSeed,
                outputDirectory
        );

        System.out.println("experiment=" + experimentCatalog.name());
        System.out.println("simulation_count_per_case=" + simulationCount);
        System.out.println("random_seed=" + randomSeed);
        System.out.println("output_directory=" + outputDirectory.toAbsolutePath());
    }

    private SimulationExperimentCatalog getExperimentCatalog() {
        String experiment = System.getenv("ASSESSMENT_SIMULATION_EXPERIMENT");
        return experiment == null
                ? SimulationExperimentCatalog.SMALL_TRADE_DEAD_ZONE_FREQUENCY
                : SimulationExperimentCatalog.valueOf(experiment);
    }

    private int getSimulationCount() {
        String simulationCount = System.getenv("ASSESSMENT_SIMULATION_EXPERIMENT_COUNT");
        return simulationCount == null
                ? DEFAULT_SIMULATION_COUNT
                : Integer.parseInt(simulationCount);
    }

    private long getRandomSeed() {
        String randomSeed = System.getenv("ASSESSMENT_SIMULATION_EXPERIMENT_SEED");
        return randomSeed == null
                ? DEFAULT_RANDOM_SEED
                : Long.parseLong(randomSeed);
    }
}
