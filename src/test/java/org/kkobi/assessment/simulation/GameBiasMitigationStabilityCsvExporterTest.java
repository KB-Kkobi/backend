package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.service.ScenarioService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBiasMitigationStabilityCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    void exportSeedStabilityAndAxisCorrelation() throws Exception {
        List<Long> randomSeeds = List.of(20260826L, 20260827L, 20260828L);

        new GameBiasMitigationStabilityCsvExporter().exportStability(
                new ScenarioService().getScenario("SC001"),
                100,
                randomSeeds,
                outputDirectory
        );

        Path seedDetailPath = outputDirectory.resolve(
                GameBiasMitigationStabilityCsvExporter.SEED_DETAIL_FILE_NAME
        );
        Path personaStabilityPath = outputDirectory.resolve(
                GameBiasMitigationStabilityCsvExporter.PERSONA_STABILITY_FILE_NAME
        );
        Path axisCorrelationPath = outputDirectory.resolve(
                GameBiasMitigationStabilityCsvExporter.AXIS_CORRELATION_FILE_NAME
        );
        assertTrue(Files.exists(seedDetailPath));
        assertTrue(Files.exists(personaStabilityPath));
        assertTrue(Files.exists(axisCorrelationPath));
        assertEquals(
                randomSeeds.size() + 1,
                Files.readAllLines(seedDetailPath, StandardCharsets.UTF_8).size()
        );
        assertEquals(
                PersonaType.values().length + 1,
                Files.readAllLines(personaStabilityPath, StandardCharsets.UTF_8).size()
        );
        assertEquals(
                1 + (randomSeeds.size() + 1) * 3,
                Files.readAllLines(axisCorrelationPath, StandardCharsets.UTF_8).size()
        );
    }
}
