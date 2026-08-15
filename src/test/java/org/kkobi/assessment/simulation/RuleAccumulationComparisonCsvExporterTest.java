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

class RuleAccumulationComparisonCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    @DisplayName("무제한·P95 상한·50% 감쇠 방식의 성향 분포를 CSV로 비교한다.")
    void exportRuleAccumulationComparison() throws IOException {
        new RuleAccumulationComparisonCsvExporter().exportRuleAccumulationComparison(
                new ScenarioService().getScenario("SC001"),
                30,
                20260815L,
                outputDirectory
        );

        Path comparisonPath = outputDirectory.resolve(
                RuleAccumulationComparisonCsvExporter.COMPARISON_FILE_NAME
        );
        List<String> lines = Files.readAllLines(comparisonPath, StandardCharsets.UTF_8);

        assertEquals(
                GameBehaviorFrequencyCondition.values().length
                        * RuleAccumulationCondition.values().length + 1,
                lines.size()
        );
        assertTrue(lines.get(0).contains("규칙_누적_조건"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("P95 이후 50% 감쇠")));
    }
}
