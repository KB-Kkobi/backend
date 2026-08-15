package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kkobi.game.service.ScenarioService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationComparisonCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    void exportConfiguredExperimentCases() throws Exception {
        List<SimulationExperimentCase> experimentCases = SimulationExperimentCatalog
                .SMALL_TRADE_DEAD_ZONE_FREQUENCY
                .getExperimentCases();
        Map<String, GameBehaviorSimulationAnalysis> analyses =
                new SimulationComparisonCsvExporter().exportComparison(
                        new ScenarioService().getScenario("SC001"),
                        experimentCases,
                        100,
                        20260826L,
                        outputDirectory
                );

        Path outputPath = outputDirectory.resolve(SimulationComparisonCsvExporter.FILE_NAME);
        assertTrue(Files.exists(outputPath));
        assertEquals(experimentCases.size(), analyses.size());
        assertEquals(
                experimentCases.size() + 1,
                Files.readAllLines(outputPath, StandardCharsets.UTF_8).size()
        );
    }
}
