package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
    @DisplayName("개인별 시뮬레이션 결과와 성향별 요약을 CSV로 저장한다.")
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
        assertTrue(Files.exists(simulationResultPath));
        assertTrue(Files.exists(personaSummaryPath));
        assertEquals(
                simulationCount + 1,
                Files.readAllLines(simulationResultPath, StandardCharsets.UTF_8).size()
        );
        assertEquals(
                10,
                Files.readAllLines(personaSummaryPath, StandardCharsets.UTF_8).size()
        );
        assertEquals(simulationCount, analysis.getTotalSimulationCount());
    }
}
