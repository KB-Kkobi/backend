package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kkobi.game.service.ScenarioService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "ASSESSMENT_PERSONA_PROFILE_ENABLED", matches = "true")
class PersonaProfileSimulationCsvExecutionTest {

    private static final int DEFAULT_USER_COUNT_PER_PERSONA = 100;
    private static final long[] DEFAULT_SEEDS = {20260816L, 20260817L, 20260818L};
    private static final DateTimeFormatter DIRECTORY_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Test
    @DisplayName("유형별 판정 결과와 Seed 안정성 결과를 CSV로 출력한다.")
    void exportPersonaProfileSimulationCsv() {
        int userCount = getUserCountPerPersona();
        GameRuleEvaluationCondition ruleEvaluationCondition = getRuleEvaluationCondition();
        PersonaProfileSimulationRunner runner = new PersonaProfileSimulationRunner();
        PersonaProfileSimulationCsvExporter exporter =
                new PersonaProfileSimulationCsvExporter();
        List<PersonaProfileSimulationResult> allResults = new ArrayList<>();
        Path root = Path.of(
                "build",
                "assessment-simulation",
                "persona-profiles-" + ruleEvaluationCondition.name().toLowerCase()
                        + "-" + LocalDateTime.now().format(DIRECTORY_TIMESTAMP_FORMATTER)
        );

        for (long seed : getSeeds()) {
            List<PersonaProfileSimulationResult> results = runner.run(
                    new ScenarioService().getScenario("SC001"),
                    userCount,
                    seed,
                    ruleEvaluationCondition
            );
            exporter.export(root.resolve("seed-" + seed), results);
            allResults.addAll(results);
        }
        exporter.exportSeedStability(root.resolve("seed-stability.csv"), allResults);

        assertEquals(userCount * 8 * getSeeds().length, allResults.size());
    }

    private int getUserCountPerPersona() {
        String value = System.getenv("ASSESSMENT_PERSONA_PROFILE_COUNT");
        return value == null ? DEFAULT_USER_COUNT_PER_PERSONA : Integer.parseInt(value);
    }

    private long[] getSeeds() {
        String value = System.getenv("ASSESSMENT_PERSONA_PROFILE_SEEDS");
        if (value == null || value.isBlank()) {
            return DEFAULT_SEEDS;
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .mapToLong(Long::parseLong)
                .toArray();
    }

    private GameRuleEvaluationCondition getRuleEvaluationCondition() {
        String value = System.getenv("ASSESSMENT_PERSONA_PROFILE_RULE");
        return value == null
                ? GameRuleEvaluationCondition.LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP
                : GameRuleEvaluationCondition.valueOf(value);
    }
}
