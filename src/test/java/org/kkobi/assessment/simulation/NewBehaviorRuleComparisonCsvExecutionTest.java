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
    @DisplayName("정상장 부분 매도 규칙의 단계별 정제 효과를 비교한다.")
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
                        "all-targeted-rules",
                        "위험 예산 유지·HHL·HLL·LHH 표적 규칙 전체",
                        GameBiasMitigationCondition.BALANCED_CAP_WITH_ALL_TARGETED_RULES,
                        GameRuleEvaluationCondition.BALANCED_CAP_WITH_ALL_TARGETED_RULES
                ),
                new RuleCase(
                        "normal-partial-sell",
                        "표적 규칙·정상장 의미있는 부분 매도",
                        GameBiasMitigationCondition.ALL_TARGETED_WITH_NORMAL_PARTIAL_SELL,
                        GameRuleEvaluationCondition.ALL_TARGETED_WITH_NORMAL_PARTIAL_SELL
                ),
                new RuleCase(
                        "partial-sell-stock-limit",
                        "부분 매도·매도 전 주식 70% 미만",
                        GameBiasMitigationCondition
                                .ALL_TARGETED_WITH_PARTIAL_SELL_STOCK_LIMIT,
                        GameRuleEvaluationCondition
                                .ALL_TARGETED_WITH_PARTIAL_SELL_STOCK_LIMIT
                ),
                new RuleCase(
                        "partial-sell-stock-limit-exclusive",
                        "부분 매도·매도 전 주식 70% 미만·HLL 비추격 상호 배타",
                        GameBiasMitigationCondition
                                .ALL_TARGETED_WITH_PARTIAL_SELL_STOCK_LIMIT_EXCLUSIVE,
                        GameRuleEvaluationCondition
                                .ALL_TARGETED_WITH_PARTIAL_SELL_STOCK_LIMIT_EXCLUSIVE
                ),
                new RuleCase(
                        "partial-sell-cash-band",
                        "부분 매도·주식 제한·매도 후 현금 25~50%",
                        GameBiasMitigationCondition
                                .ALL_TARGETED_WITH_PARTIAL_SELL_CASH_BAND,
                        GameRuleEvaluationCondition
                                .ALL_TARGETED_WITH_PARTIAL_SELL_CASH_BAND
                ),
                new RuleCase(
                        "partial-sell-cash-retention",
                        "부분 매도·주식 제한·현금 2 Tick 유지",
                        GameBiasMitigationCondition
                                .ALL_TARGETED_WITH_PARTIAL_SELL_CASH_RETENTION,
                        GameRuleEvaluationCondition
                                .ALL_TARGETED_WITH_PARTIAL_SELL_CASH_RETENTION
                ),
                new RuleCase(
                        "partial-sell-exclusive",
                        "부분 매도 최종 조건·HLL 비추격 상호 배타",
                        GameBiasMitigationCondition.ALL_TARGETED_WITH_PARTIAL_SELL_EXCLUSIVE,
                        GameRuleEvaluationCondition.ALL_TARGETED_WITH_PARTIAL_SELL_EXCLUSIVE
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
