package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kkobi.game.service.ScenarioService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBehaviorFrequencyComparisonCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    @DisplayName("저빈도·중간 빈도·고빈도 조건의 성향 분포를 CSV로 비교한다.")
    void exportFrequencyComparison() throws IOException {
        Map<GameBehaviorFrequencyCondition, GameBehaviorSimulationAnalysis> analyses =
                new GameBehaviorFrequencyComparisonCsvExporter().exportFrequencyComparison(
                        new ScenarioService().getScenario("SC001"),
                        30,
                        20260815L,
                        outputDirectory
                );

        Path comparisonPath = outputDirectory.resolve(
                GameBehaviorFrequencyComparisonCsvExporter.FREQUENCY_COMPARISON_FILE_NAME
        );
        List<String> lines = Files.readAllLines(comparisonPath, StandardCharsets.UTF_8);

        assertEquals(GameBehaviorFrequencyCondition.values().length, analyses.size());
        assertEquals(GameBehaviorFrequencyCondition.values().length + 1, lines.size());
        assertTrue(lines.get(0).contains("빈도_조건_설명"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("저빈도")));
        assertTrue(Files.exists(outputDirectory.resolve("low").resolve(
                GameBehaviorSimulationCsvExporter.BIAS_CAUSE_SUMMARY_FILE_NAME
        )));
    }
}
