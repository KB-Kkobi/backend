package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kkobi.game.service.ScenarioService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBiasMitigationFrequencyComparisonCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    void exportCurrentMitigationRuleDistributionByFrequency() throws Exception {
        Map<GameBehaviorFrequencyCondition, GameBehaviorSimulationAnalysis> analyses =
                new GameBiasMitigationFrequencyComparisonCsvExporter().exportComparison(
                        new ScenarioService().getScenario("SC001"),
                        100,
                        20260826L,
                        outputDirectory
                );

        Path outputPath = outputDirectory.resolve(
                GameBiasMitigationFrequencyComparisonCsvExporter.FILE_NAME
        );
        assertTrue(Files.exists(outputPath));
        assertEquals(GameBehaviorFrequencyCondition.values().length, analyses.size());
        assertEquals(
                GameBehaviorFrequencyCondition.values().length + 1,
                Files.readAllLines(outputPath, StandardCharsets.UTF_8).size()
        );
    }
}
