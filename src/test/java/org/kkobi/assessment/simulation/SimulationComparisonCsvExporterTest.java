package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kkobi.game.service.ScenarioService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationComparisonCsvExporterTest {

    @TempDir
    Path outputDirectory;

    @Test
    void exportConfiguredExperimentCases() throws Exception {
        List<SimulationExperimentCase> experimentCases = SimulationExperimentCatalog
                .SMALL_TRADE_DEAD_ZONE_FREQUENCY
                .getExperimentCases();
        Map<String, GameBehaviorSimulationAnalysis> analyses =
                new SimulationComparisonCsvExporter().exportComparison(
                        new ScenarioService().getScenario("SC001"),
                        experimentCases,
                        100,
                        20260826L,
                        outputDirectory
                );

        Path outputPath = outputDirectory.resolve(SimulationComparisonCsvExporter.FILE_NAME);
        assertTrue(Files.exists(outputPath));
        assertEquals(experimentCases.size(), analyses.size());
        assertEquals(
                experimentCases.size() + 1,
                Files.readAllLines(outputPath, StandardCharsets.UTF_8).size()
        );
    }

    @Test
    void configureRecommendedGameRepetitionPolicy() {
        GameBiasMitigationCondition condition =
                GameBiasMitigationCondition.RECOMMENDED_GAME_REPETITION_POLICY;

        assertEquals(
                SameTickRuleApplicationCondition.ONCE_PER_TICK,
                condition.getSameTickRuleCondition()
        );
        assertEquals(
                RuleAccumulationCondition.DOMINANT_BUY_RULE_HARD_CAP,
                condition.getRuleAccumulationCondition()
        );
        assertEquals(
                ConsecutiveActionMultiplierCondition.DISABLED,
                condition.getMultiplierCondition()
        );
        assertEquals(
                2,
                condition.getRuleAccumulationCondition().getApplicationLimit(
                        org.kkobi.assessment.enums.BehaviorRuleCode.LOSS_AVERAGING_BUY
                )
        );
        assertEquals(
                4,
                condition.getRuleAccumulationCondition().getApplicationLimit(
                        org.kkobi.assessment.enums.BehaviorRuleCode.CRASH_BUY
                )
        );
        assertEquals(
                2,
                condition.getRuleAccumulationCondition().getApplicationLimit(
                        org.kkobi.assessment.enums.BehaviorRuleCode.BULL_BUY
                )
        );
        assertEquals(
                Integer.MAX_VALUE,
                condition.getRuleAccumulationCondition().getApplicationLimit(
                        org.kkobi.assessment.enums.BehaviorRuleCode.LOSS_CUT_SELL
                )
        );
    }

    @Test
    void configureCandidateRuleComparisonCases() {
        List<SimulationExperimentCase> experimentCases = SimulationExperimentCatalog
                .REPETITION_POLICY_AND_CANDIDATE_RULES_FREQUENCY
                .getExperimentCases();
        GameRuleEvaluationCondition candidateRules = GameBiasMitigationCondition
                .THREE_CANDIDATE_RULES
                .getRuleEvaluationCondition();
        GameBiasMitigationCondition combined = GameBiasMitigationCondition
                .RECOMMENDED_REPETITION_POLICY_AND_THREE_CANDIDATE_RULES;

        assertEquals(9, experimentCases.size());
        assertTrue(candidateRules.appliesCrashHoldingRule());
        assertTrue(candidateRules.appliesNormalPlannedBuyRule());
        assertTrue(candidateRules.appliesCashBufferMaintenanceRule());
        assertEquals(
                RuleAccumulationCondition.DOMINANT_BUY_RULE_HARD_CAP,
                combined.getRuleAccumulationCondition()
        );
        assertEquals(
                ConsecutiveActionMultiplierCondition.DISABLED,
                combined.getMultiplierCondition()
        );
    }
}
