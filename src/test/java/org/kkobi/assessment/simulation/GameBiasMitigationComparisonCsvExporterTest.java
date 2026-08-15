package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.game.service.ScenarioService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBiasMitigationComparisonCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    void exportBiasMitigationSummaryAndRuleComparison() throws Exception {
        new GameBiasMitigationComparisonCsvExporter().exportComparison(
                new ScenarioService().getScenario("SC001"),
                100,
                20260826L,
                outputDirectory
        );

        Path summaryPath = outputDirectory.resolve(
                GameBiasMitigationComparisonCsvExporter.COMPARISON_FILE_NAME
        );
        Path rulePath = outputDirectory.resolve(
                GameBiasMitigationComparisonCsvExporter.RULE_COMPARISON_FILE_NAME
        );
        assertTrue(Files.exists(summaryPath));
        assertTrue(Files.exists(rulePath));
        assertEquals(
                GameBiasMitigationCondition.values().length + 1,
                Files.readAllLines(summaryPath, StandardCharsets.UTF_8).size()
        );
        assertEquals(
                1 + GameBiasMitigationCondition.values().length
                        * BehaviorRuleCode.values().length,
                Files.readAllLines(rulePath, StandardCharsets.UTF_8).size()
        );
    }
}
