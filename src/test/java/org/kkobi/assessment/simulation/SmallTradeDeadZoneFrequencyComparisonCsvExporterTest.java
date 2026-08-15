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

class SmallTradeDeadZoneFrequencyComparisonCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    void exportSmallTradeDeadZoneDistributionByFrequency() throws Exception {
        Map<SmallTradeDeadZoneFrequencyComparisonCsvExporter.ComparisonKey,
                GameBehaviorSimulationAnalysis> analyses =
                new SmallTradeDeadZoneFrequencyComparisonCsvExporter().exportComparison(
                        new ScenarioService().getScenario("SC001"),
                        100,
                        20260826L,
                        outputDirectory
                );

        Path outputPath = outputDirectory.resolve(
                SmallTradeDeadZoneFrequencyComparisonCsvExporter.FILE_NAME
        );
        int expectedConditionCount = GameBehaviorFrequencyCondition.values().length * 2;
        assertTrue(Files.exists(outputPath));
        assertEquals(expectedConditionCount, analyses.size());
        assertEquals(
                expectedConditionCount + 1,
                Files.readAllLines(outputPath, StandardCharsets.UTF_8).size()
        );
    }
}
