package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.game.service.ScenarioService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleApplicationPercentileCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    @DisplayName("빈도 조건별 사용자 규칙 적용 횟수 백분위를 CSV로 저장한다.")
    void exportRuleApplicationPercentiles() throws IOException {
        new RuleApplicationPercentileCsvExporter().exportRuleApplicationPercentiles(
                new ScenarioService().getScenario("SC001"),
                30,
                20260815L,
                outputDirectory
        );

        Path percentilePath = outputDirectory.resolve(
                RuleApplicationPercentileCsvExporter.RULE_PERCENTILE_FILE_NAME
        );
        List<String> lines = Files.readAllLines(percentilePath, StandardCharsets.UTF_8);

        assertEquals(
                GameBehaviorFrequencyCondition.values().length
                        * BehaviorRuleCode.values().length + 1,
                lines.size()
        );
        assertTrue(lines.get(0).contains("적용_사용자_P95"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("급등장에서 주식 추격 매수")));
    }
}
