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

class TradeQuantityGenerationComparisonCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    void exportCurrentAndSymmetricTradeQuantityComparison() throws Exception {
        new TradeQuantityGenerationComparisonCsvExporter().exportComparison(
                new ScenarioService().getScenario("SC001"),
                100,
                20260825L,
                outputDirectory
        );

        Path comparisonPath = outputDirectory.resolve(
                TradeQuantityGenerationComparisonCsvExporter.COMPARISON_FILE_NAME
        );
        Path ruleComparisonPath = outputDirectory.resolve(
                TradeQuantityGenerationComparisonCsvExporter.RULE_COMPARISON_FILE_NAME
        );
        assertTrue(Files.exists(comparisonPath));
        assertTrue(Files.exists(ruleComparisonPath));
        List<String> lines = Files.readAllLines(comparisonPath, StandardCharsets.UTF_8);
        assertEquals(TradeQuantityGenerationCondition.values().length + 1, lines.size());
        assertTrue(lines.stream().anyMatch(line -> line.startsWith(
                TradeQuantityGenerationCondition.CURRENT_RANDOM_BUY_PERCENTAGE.name()
        )));
        assertTrue(lines.stream().anyMatch(line -> line.startsWith(
                TradeQuantityGenerationCondition.SYMMETRIC_THREE_LEVEL.name()
        )));
        List<String> ruleLines = Files.readAllLines(
                ruleComparisonPath,
                StandardCharsets.UTF_8
        );
        assertEquals(
                1 + TradeQuantityGenerationCondition.values().length
                        * BehaviorRuleCode.values().length,
                ruleLines.size()
        );
    }
}
