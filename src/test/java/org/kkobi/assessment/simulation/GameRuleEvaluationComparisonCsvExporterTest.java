package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.game.service.ScenarioService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRuleEvaluationComparisonCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    void exportRuleApplicationPercentilesAndAxisContributions() throws Exception {
        GameRuleEvaluationComparisonCsvExporter exporter =
                new GameRuleEvaluationComparisonCsvExporter();

        Map<GameRuleEvaluationCondition, GameBehaviorSimulationAnalysis> analyses =
                exporter.exportComparison(
                        new ScenarioService().getScenario("SC001"),
                        100,
                        20260825L,
                        outputDirectory
                );

        Path ruleStatisticsPath = outputDirectory.resolve(
                GameRuleEvaluationComparisonCsvExporter.RULE_STATISTICS_FILE_NAME
        );
        assertTrue(Files.exists(ruleStatisticsPath));

        List<String> lines = Files.readAllLines(ruleStatisticsPath, StandardCharsets.UTF_8);
        assertEquals(
                1 + GameRuleEvaluationCondition.values().length * BehaviorRuleCode.values().length,
                lines.size()
        );

        String conditionName = GameRuleEvaluationCondition
                .EXCLUSIVE_MODERATE_FIXED_ACTION_SCORE_AND_DEPOSIT_DECISION
                .name();
        String crashBuyRow = lines.stream()
                .filter(line -> line.startsWith(conditionName + ","))
                .filter(line -> line.contains("," + BehaviorRuleCode.CRASH_BUY.name() + ","))
                .findFirst()
                .orElseThrow();
        String[] columns = crashBuyRow.split(",", -1);
        long expectedApplicationCount = analyses.get(
                        GameRuleEvaluationCondition
                                .EXCLUSIVE_MODERATE_FIXED_ACTION_SCORE_AND_DEPOSIT_DECISION
                )
                .getRuleStatistics()
                .get(BehaviorRuleCode.CRASH_BUY)
                .getApplicationCount();

        assertEquals(expectedApplicationCount, Long.parseLong(columns[5]));
        BigDecimal median = new BigDecimal(columns[8]);
        BigDecimal percentile90 = new BigDecimal(columns[9]);
        BigDecimal percentile95 = new BigDecimal(columns[10]);
        assertTrue(median.compareTo(percentile90) <= 0);
        assertTrue(percentile90.compareTo(percentile95) <= 0);
    }
}
