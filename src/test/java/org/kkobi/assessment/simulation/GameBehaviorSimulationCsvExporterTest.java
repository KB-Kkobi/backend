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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBehaviorSimulationCsvExporterTest {

    @TempDir
    Path outputDirectory;

    private final ScenarioService scenarioService = new ScenarioService();
    private final GameBehaviorSimulationCsvExporter csvExporter =
            new GameBehaviorSimulationCsvExporter();

    @Test
    @DisplayName("개인별 결과와 성향 분포 및 편중 원인 통계를 CSV로 저장한다.")
    void exportGameSimulations() throws IOException {
        int simulationCount = 20;

        GameBehaviorSimulationAnalysis analysis = csvExporter.exportGameSimulations(
                scenarioService.getScenario("SC001"),
                simulationCount,
                20260815L,
                outputDirectory
        );

        Path simulationResultPath = outputDirectory.resolve(
                GameBehaviorSimulationCsvExporter.SIMULATION_RESULT_FILE_NAME
        );
        Path personaSummaryPath = outputDirectory.resolve(
                GameBehaviorSimulationCsvExporter.PERSONA_SUMMARY_FILE_NAME
        );
        Path biasCauseSummaryPath = outputDirectory.resolve(
                GameBehaviorSimulationCsvExporter.BIAS_CAUSE_SUMMARY_FILE_NAME
        );
        Path ruleStatisticsPath = outputDirectory.resolve(
                GameBehaviorSimulationCsvExporter.RULE_STATISTICS_FILE_NAME
        );
        assertTrue(Files.exists(simulationResultPath));
        assertTrue(Files.exists(personaSummaryPath));
        assertTrue(Files.exists(biasCauseSummaryPath));
        assertTrue(Files.exists(ruleStatisticsPath));
        assertEquals(
                simulationCount + 1,
                Files.readAllLines(simulationResultPath, StandardCharsets.UTF_8).size()
        );
        assertEquals(
                10,
                Files.readAllLines(personaSummaryPath, StandardCharsets.UTF_8).size()
        );
        assertEquals(
                13,
                Files.readAllLines(biasCauseSummaryPath, StandardCharsets.UTF_8).size()
        );
        assertEquals(
                BehaviorRuleCode.values().length + 1,
                Files.readAllLines(ruleStatisticsPath, StandardCharsets.UTF_8).size()
        );
        assertTrue(Files.readString(biasCauseSummaryPath, StandardCharsets.UTF_8)
                .contains("사용자당 평균 매수 횟수"));
        assertTrue(Files.readString(ruleStatisticsPath, StandardCharsets.UTF_8)
                .contains("급락장에서 주식 추가 매수"));
        assertEquals(simulationCount, analysis.getTotalSimulationCount());
    }
}
