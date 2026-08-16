package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kkobi.game.service.ScenarioService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonaProfileSimulationCsvExporterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("유형별 결과와 혼동 행렬 CSV를 생성한다.")
    void exportPersonaProfileCsv() {
        List<PersonaProfileSimulationResult> results =
                new PersonaProfileSimulationRunner().run(
                        new ScenarioService().getScenario("SC001"),
                        1,
                        20260816L
                );

        new PersonaProfileSimulationCsvExporter().export(temporaryDirectory, results);

        assertTrue(Files.exists(temporaryDirectory.resolve("persona-profile-results.csv")));
        assertTrue(Files.exists(temporaryDirectory.resolve("persona-confusion-matrix.csv")));
        assertTrue(Files.exists(
                temporaryDirectory.resolve("persona-standardization-summary.csv")));
        assertTrue(Files.exists(
                temporaryDirectory.resolve("persona-standardized-results.csv")));
    }
}
