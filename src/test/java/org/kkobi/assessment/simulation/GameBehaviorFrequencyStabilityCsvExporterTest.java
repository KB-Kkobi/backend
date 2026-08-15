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

class GameBehaviorFrequencyStabilityCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    @DisplayName("여러 Seed의 빈도별 성향 비율 평균과 표준편차를 CSV로 저장한다.")
    void exportFrequencyStability() throws IOException {
        List<Long> randomSeeds = List.of(20260815L, 20260816L, 20260817L);

        new GameBehaviorFrequencyStabilityCsvExporter().exportFrequencyStability(
                new ScenarioService().getScenario("SC001"),
                30,
                randomSeeds,
                outputDirectory
        );

        Path detailPath = outputDirectory.resolve(
                GameBehaviorFrequencyStabilityCsvExporter.SEED_DETAIL_FILE_NAME
        );
        Path summaryPath = outputDirectory.resolve(
                GameBehaviorFrequencyStabilityCsvExporter.SEED_SUMMARY_FILE_NAME
        );
        List<String> detailLines = Files.readAllLines(detailPath, StandardCharsets.UTF_8);
        List<String> summaryLines = Files.readAllLines(summaryPath, StandardCharsets.UTF_8);

        assertEquals(
                randomSeeds.size() * GameBehaviorFrequencyCondition.values().length + 1,
                detailLines.size()
        );
        assertEquals(GameBehaviorFrequencyCondition.values().length + 1, summaryLines.size());
        assertTrue(summaryLines.get(0).contains("표준편차"));
    }
}
