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

class RuleAxisContributionAnalysisCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    @DisplayName("행동 종류와 시장 상황별 RT·LH·RP 기여도를 CSV로 저장한다.")
    void exportRuleAxisContributionAnalysis() throws IOException {
        new RuleAxisContributionAnalysisCsvExporter().exportRuleAxisContributionAnalysis(
                new ScenarioService().getScenario("SC001"),
                30,
                20260815L,
                outputDirectory
        );

        Path detailPath = outputDirectory.resolve(
                RuleAxisContributionAnalysisCsvExporter.RULE_DETAIL_FILE_NAME
        );
        Path summaryPath = outputDirectory.resolve(
                RuleAxisContributionAnalysisCsvExporter.GROUP_SUMMARY_FILE_NAME
        );
        List<String> detailLines = Files.readAllLines(detailPath, StandardCharsets.UTF_8);
        List<String> summaryLines = Files.readAllLines(summaryPath, StandardCharsets.UTF_8);

        assertEquals(
                GameBehaviorFrequencyCondition.values().length
                        * BehaviorRuleCode.values().length + 1,
                detailLines.size()
        );
        assertTrue(summaryLines.get(0).contains("RT_양수_기여"));
        assertTrue(summaryLines.stream().anyMatch(line -> line.contains("매수 행동")));
        assertTrue(summaryLines.stream().anyMatch(line -> line.contains("급등장")));
    }
}
