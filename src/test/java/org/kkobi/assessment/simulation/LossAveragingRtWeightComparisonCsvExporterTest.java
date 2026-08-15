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

class LossAveragingRtWeightComparisonCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    @DisplayName("물타기 RT 가중치별 성향 분포와 점수 진단을 CSV로 저장한다.")
    void exportLossAveragingRtWeightComparison() throws IOException {
        new LossAveragingRtWeightComparisonCsvExporter()
                .exportLossAveragingRtWeightComparison(
                        new ScenarioService().getScenario("SC001"),
                        30,
                        20260815L,
                        outputDirectory
                );

        Path comparisonPath = outputDirectory.resolve(
                LossAveragingRtWeightComparisonCsvExporter.COMPARISON_FILE_NAME
        );
        List<String> lines = Files.readAllLines(
                comparisonPath,
                StandardCharsets.UTF_8
        );

        assertEquals(
                GameBehaviorFrequencyCondition.values().length
                        * LossAveragingRtWeightCondition.values().length + 1,
                lines.size()
        );
        assertTrue(lines.get(0).contains("물타기_RT_가중치"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("RT_15")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("RT_10")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("RT_5")));
    }
}
