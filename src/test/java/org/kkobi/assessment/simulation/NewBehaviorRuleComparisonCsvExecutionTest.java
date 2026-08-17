package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.service.ScenarioService;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(
        named = "ASSESSMENT_NEW_RULE_COMPARISON_ENABLED",
        matches = "true"
)
class NewBehaviorRuleComparisonCsvExecutionTest {

    private static final int NEUTRAL_USER_COUNT = 10_000;
    private static final int PROFILE_USER_COUNT = 100;
    private static final long NEUTRAL_SEED = 20260826L;
    private static final long[] PROFILE_SEEDS = {20260816L, 20260817L, 20260818L};
    private static final DateTimeFormatter OUTPUT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Test
    @DisplayName("HHL·HLL·LHH 표적 행동 규칙을 비교한다.")
    void exportNewRuleComparison() {
        ScenarioDto scenario = new ScenarioService().getScenario("SC001");
        Path root = Path.of(
                "build",
                "assessment-simulation",
                "new-behavior-rules-" + LocalDateTime.now().format(OUTPUT_FORMATTER)
        );

        exportNeutralComparison(scenario, root.resolve("neutral"));
        exportProfileComparison(scenario, root.resolve("profiles"));
    }

    private void exportNeutralComparison(ScenarioDto scenario, Path outputDirectory) {
        List<SimulationExperimentCase> cases = ruleCases().stream()
                .map(ruleCase -> new SimulationExperimentCase(
                        ruleCase.name(),
                        ruleCase.description(),
                        ruleCase.mitigationCondition(),
                        GameBehaviorFrequencyCondition.MEDIUM
                ))
                .toList();

        assertEquals(cases.size(), new SimulationComparisonCsvExporter().exportComparison(
                scenario,
                cases,
                NEUTRAL_USER_COUNT,
                NEUTRAL_SEED,
                outputDirectory
        ).size());
    }

    private void exportProfileComparison(ScenarioDto scenario, Path outputDirectory) {
        PersonaProfileSimulationRunner runner = new PersonaProfileSimulationRunner();
        PersonaProfileSimulationCsvExporter exporter =
                new PersonaProfileSimulationCsvExporter();

        for (RuleCase ruleCase : ruleCases()) {
            List<PersonaProfileSimulationResult> results = new ArrayList<>();
            for (long seed : PROFILE_SEEDS) {
                List<PersonaProfileSimulationResult> seedResults = runner.run(
                        scenario,
                        PROFILE_USER_COUNT,
                        seed,
                        ruleCase.ruleCondition()
                );
                exporter.export(
                        outputDirectory.resolve(ruleCase.name()).resolve("seed-" + seed),
                        seedResults
                );
                results.addAll(seedResults);
            }
            exporter.exportSeedStability(
                    outputDirectory.resolve(ruleCase.name()).resolve("seed-stability.csv"),
                    results
            );
            assertEquals(PROFILE_USER_COUNT * 8 * PROFILE_SEEDS.length, results.size());
        }
    }

    private List<RuleCase> ruleCases() {
        return List.of(
                new RuleCase(
                        "current",
                        "현재 균형 상한",
                        GameBiasMitigationCondition.LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP,
                        GameRuleEvaluationCondition.LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP
                ),
                new RuleCase(
                        "risk-budget",
                        "위험 예산 유지",
                        GameBiasMitigationCondition.BALANCED_CAP_WITH_RISK_BUDGET_MAINTENANCE,
                        GameRuleEvaluationCondition.BALANCED_CAP_WITH_RISK_BUDGET_MAINTENANCE
                ),
                new RuleCase(
                        "risk-budget-and-hhl-composite",
                        "위험 예산 유지·HHL 복합 규칙",
                        GameBiasMitigationCondition
                                .BALANCED_CAP_WITH_HHL_COMPOSITE,
                        GameRuleEvaluationCondition
                                .BALANCED_CAP_WITH_HHL_COMPOSITE
                ),
                new RuleCase(
                        "risk-budget-and-hll-capped-no-chase",
                        "위험 예산 유지·HLL 정제 비추격 최대 1회",
                        GameBiasMitigationCondition
                                .BALANCED_CAP_WITH_HLL_CAPPED_NO_CHASE,
                        GameRuleEvaluationCondition
                                .BALANCED_CAP_WITH_HLL_CAPPED_NO_CHASE
                ),
                new RuleCase(
                        "risk-budget-and-lhh-completed-opportunity",
                        "위험 예산 유지·LHH 완결형 기회 실행 최대 1회",
                        GameBiasMitigationCondition
                                .BALANCED_CAP_WITH_LHH_COMPLETED_OPPORTUNITY,
                        GameRuleEvaluationCondition
                                .BALANCED_CAP_WITH_LHH_COMPLETED_OPPORTUNITY
                ),
                new RuleCase(
                        "all-targeted-rules",
                        "위험 예산 유지·HHL·HLL·LHH 표적 규칙 전체",
                        GameBiasMitigationCondition.BALANCED_CAP_WITH_ALL_TARGETED_RULES,
                        GameRuleEvaluationCondition.BALANCED_CAP_WITH_ALL_TARGETED_RULES
                )
        );
    }

    private record RuleCase(
            String name,
            String description,
            GameBiasMitigationCondition mitigationCondition,
            GameRuleEvaluationCondition ruleCondition) {
    }
}
