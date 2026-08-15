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

class SameTickRuleApplicationComparisonCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    @DisplayName("같은 Tick의 동일 규칙 반복 적용 여부에 따른 결과를 CSV로 비교한다.")
    void exportSameTickRuleComparison() throws IOException {
        new SameTickRuleApplicationComparisonCsvExporter().exportSameTickRuleComparison(
                new ScenarioService().getScenario("SC001"),
                30,
                20260815L,
                outputDirectory
        );

        Path comparisonPath = outputDirectory.resolve(
                SameTickRuleApplicationComparisonCsvExporter.COMPARISON_FILE_NAME
        );
        List<String> lines = Files.readAllLines(comparisonPath, StandardCharsets.UTF_8);

        assertEquals(
                GameBehaviorFrequencyCondition.values().length
                        * SameTickRuleApplicationCondition.values().length + 1,
                lines.size()
        );
        assertTrue(lines.get(0).contains("기존_대비_규칙_적용수_차이"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Tick당 1회")));
    }
}
