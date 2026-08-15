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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsecutiveActionMultiplierComparisonCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    @DisplayName("연속 행동 배율 적용 여부에 따른 성향 분포를 CSV로 비교한다.")
    void exportMultiplierComparison() throws IOException {
        new ConsecutiveActionMultiplierComparisonCsvExporter().exportMultiplierComparison(
                new ScenarioService().getScenario("SC001"),
                30,
                20260815L,
                outputDirectory
        );

        Path comparisonPath = outputDirectory.resolve(
                ConsecutiveActionMultiplierComparisonCsvExporter.COMPARISON_FILE_NAME
        );
        List<String> lines = Files.readAllLines(comparisonPath, StandardCharsets.UTF_8);

        assertEquals(
                GameBehaviorFrequencyCondition.values().length
                        * ConsecutiveActionMultiplierCondition.values().length + 1,
                lines.size()
        );
        assertTrue(lines.get(0).contains("기존_대비_HLH_비율_차이"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("연속 배율 미적용")));
    }
}
