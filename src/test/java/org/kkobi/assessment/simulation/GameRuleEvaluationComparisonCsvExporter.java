package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.game.dto.ScenarioDto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameRuleEvaluationComparisonCsvExporter {

    public static final String COMPARISON_FILE_NAME = "game-rule-evaluation-comparison.csv";
    public static final String RULE_STATISTICS_FILE_NAME =
            "game-rule-evaluation-rule-statistics.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public Map<GameRuleEvaluationCondition, GameBehaviorSimulationAnalysis> exportComparison(
            ScenarioDto scenario,
            int simulationCount,
            long randomSeed,
            Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("게임 규칙 비교 CSV 출력 경로는 필수입니다.");
        }

        EnumMap<GameRuleEvaluationCondition, Map<BehaviorRuleCode, List<Integer>>>
                applicationCountsByCondition = new EnumMap<>(GameRuleEvaluationCondition.class);
        EnumMap<GameRuleEvaluationCondition, GameBehaviorSimulationAnalysis> analyses =
                analyzeConditions(
                        scenario,
                        simulationCount,
                        randomSeed,
                        applicationCountsByCondition
                );
        writeComparison(outputDirectory, analyses);
        writeRuleStatistics(outputDirectory, analyses, applicationCountsByCondition);
        return Map.copyOf(analyses);
    }

    private EnumMap<GameRuleEvaluationCondition, GameBehaviorSimulationAnalysis>
            analyzeConditions(
                    ScenarioDto scenario,
                    int simulationCount,
                    long randomSeed,
                    Map<GameRuleEvaluationCondition, Map<BehaviorRuleCode, List<Integer>>>
                            applicationCountsByCondition) {
        EnumMap<GameRuleEvaluationCondition, GameBehaviorSimulationAnalysis> analyses =
                new EnumMap<>(GameRuleEvaluationCondition.class);
        for (GameRuleEvaluationCondition condition : GameRuleEvaluationCondition.values()) {
            EnumMap<BehaviorRuleCode, List<Integer>> applicationCounts =
                    createRuleApplicationCounts();
            analyses.put(
                    condition,
                    simulationAnalyzer.analyzeGameSimulations(
                            scenario,
                            simulationCount,
                            randomSeed,
                            GameBehaviorFrequencyCondition.MEDIUM,
                            ConsecutiveActionMultiplierCondition.ENABLED,
                            SameTickRuleApplicationCondition.REPEATED,
                            RuleAccumulationCondition.UNLIMITED,
                            LossAveragingRtWeightCondition.RT_15,
                            condition,
                            simulationResult -> addRuleApplicationCounts(
                                    applicationCounts,
                                    simulationResult
                            )
                    )
            );
            applicationCounts.values().forEach(Collections::sort);
            applicationCountsByCondition.put(condition, applicationCounts);
        }
        return analyses;
    }

    private EnumMap<BehaviorRuleCode, List<Integer>> createRuleApplicationCounts() {
        EnumMap<BehaviorRuleCode, List<Integer>> applicationCounts =
                new EnumMap<>(BehaviorRuleCode.class);
        for (BehaviorRuleCode ruleCode : BehaviorRuleCode.values()) {
            applicationCounts.put(ruleCode, new ArrayList<>());
        }
        return applicationCounts;
    }

    private void addRuleApplicationCounts(
            Map<BehaviorRuleCode, List<Integer>> applicationCounts,
            GameBehaviorSimulationResult simulationResult) {
        for (BehaviorRuleCode ruleCode : BehaviorRuleCode.values()) {
            applicationCounts.get(ruleCode).add(
                    simulationResult.getRuleApplicationCounts().getOrDefault(ruleCode, 0)
            );
        }
    }

    private void writeComparison(
            Path outputDirectory,
            Map<GameRuleEvaluationCondition, GameBehaviorSimulationAnalysis> analyses) {
        try {
            Files.createDirectories(outputDirectory);
            try (BufferedWriter writer = Files.newBufferedWriter(
                    outputDirectory.resolve(COMPARISON_FILE_NAME),
                    StandardCharsets.UTF_8
            )) {
                writer.write(UTF_8_BYTE_ORDER_MARK);
                writeCsvRow(writer, createHeader());
                GameBehaviorSimulationAnalysis baseline = analyses.get(
                        GameRuleEvaluationCondition.BASELINE
                );
                for (GameRuleEvaluationCondition condition
                        : GameRuleEvaluationCondition.values()) {
                    writeCsvRow(
                            writer,
                            createRow(condition, analyses.get(condition), baseline)
                    );
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("게임 규칙 비교 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private List<String> createHeader() {
        List<String> header = new ArrayList<>(List.of(
                "규칙_조건(rule_condition)",
                "규칙_조건_설명(rule_condition_name)",
                "사용자수(simulation_count)",
                "RT_평균(rt_average)",
                "LH_평균(lh_average)",
                "RP_평균(rp_average)",
                "RT_100점_비율(rt_maximum_rate)",
                "LH_100점_비율(lh_maximum_rate)",
                "RP_100점_비율(rp_maximum_rate)",
                "HLH_비율(hlh_rate)",
                "기존대비_HLH_차이(hlh_rate_difference)",
                "예금_해지_후_판정_적용수(deposit_decision_application_count)",
                "예금_해지_후_판정_RT_총기여도(deposit_decision_rt_contribution)",
                "예금_해지_후_판정_LH_총기여도(deposit_decision_lh_contribution)",
                "예금_해지_후_판정_RP_총기여도(deposit_decision_rp_contribution)"
        ));
        for (PersonaType personaType : PersonaType.values()) {
            header.add(personaType.name() + "_판정_비율");
        }
        return header;
    }

    private void writeRuleStatistics(
            Path outputDirectory,
            Map<GameRuleEvaluationCondition, GameBehaviorSimulationAnalysis> analyses,
            Map<GameRuleEvaluationCondition, Map<BehaviorRuleCode, List<Integer>>>
                    applicationCountsByCondition) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputDirectory.resolve(RULE_STATISTICS_FILE_NAME),
                StandardCharsets.UTF_8
        )) {
            writer.write(UTF_8_BYTE_ORDER_MARK);
            writeCsvRow(writer, createRuleStatisticsHeader());
            for (GameRuleEvaluationCondition condition : GameRuleEvaluationCondition.values()) {
                GameBehaviorSimulationAnalysis analysis = analyses.get(condition);
                Map<BehaviorRuleCode, List<Integer>> applicationCounts =
                        applicationCountsByCondition.get(condition);
                for (BehaviorRuleCode ruleCode : BehaviorRuleCode.values()) {
                    writeCsvRow(
                            writer,
                            createRuleStatisticsRow(
                                    condition,
                                    analysis,
                                    ruleCode,
                                    applicationCounts.get(ruleCode)
                            )
                    );
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("게임 규칙 기여도 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private List<String> createRuleStatisticsHeader() {
        return List.of(
                "규칙_조건(rule_condition)",
                "규칙_조건_설명(rule_condition_name)",
                "규칙_코드(rule_code)",
                "규칙_설명(rule_name)",
                "전체_사용자수(total_user_count)",
                "전체_적용수(application_count)",
                "적용_사용자수(applied_user_count)",
                "사용자당_평균_적용수(application_average)",
                "사용자당_중앙값(application_median)",
                "사용자당_P90(application_p90)",
                "사용자당_P95(application_p95)",
                "RT_총기여도(rt_total_contribution)",
                "LH_총기여도(lh_total_contribution)",
                "RP_총기여도(rp_total_contribution)"
        );
    }

    private List<String> createRuleStatisticsRow(
            GameRuleEvaluationCondition condition,
            GameBehaviorSimulationAnalysis analysis,
            BehaviorRuleCode ruleCode,
            List<Integer> applicationCounts) {
        GameBehaviorSimulationAnalysis.RuleStatistics ruleStatistics =
                analysis.getRuleStatistics().get(ruleCode);
        long appliedUserCount = applicationCounts.stream()
                .filter(applicationCount -> applicationCount > 0)
                .count();
        return List.of(
                condition.name(),
                condition.getDescription(),
                ruleCode.name(),
                GameBehaviorSimulationCsvExporter.getRuleName(ruleCode),
                String.valueOf(analysis.getTotalSimulationCount()),
                String.valueOf(ruleStatistics.getApplicationCount()),
                String.valueOf(appliedUserCount),
                calculateAverage(applicationCounts),
                calculateMedian(applicationCounts),
                String.valueOf(calculatePercentile(applicationCounts, 90)),
                String.valueOf(calculatePercentile(applicationCounts, 95)),
                ruleStatistics.getRtTotalContribution().toPlainString(),
                ruleStatistics.getLhTotalContribution().toPlainString(),
                ruleStatistics.getRpTotalContribution().toPlainString()
        );
    }

    private String calculateAverage(List<Integer> values) {
        if (values.isEmpty()) {
            return "0.0000";
        }
        long total = values.stream().mapToLong(Integer::longValue).sum();
        return BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String calculateMedian(List<Integer> sortedValues) {
        if (sortedValues.isEmpty()) {
            return "0.00";
        }
        int middleIndex = sortedValues.size() / 2;
        if (sortedValues.size() % 2 == 1) {
            return BigDecimal.valueOf(sortedValues.get(middleIndex))
                    .setScale(2)
                    .toPlainString();
        }
        return BigDecimal.valueOf(sortedValues.get(middleIndex - 1))
                .add(BigDecimal.valueOf(sortedValues.get(middleIndex)))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private int calculatePercentile(List<Integer> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        int rank = (int) Math.ceil(sortedValues.size() * percentile / 100.0);
        return sortedValues.get(Math.max(0, rank - 1));
    }

    private List<String> createRow(
            GameRuleEvaluationCondition condition,
            GameBehaviorSimulationAnalysis analysis,
            GameBehaviorSimulationAnalysis baseline) {
        GameBehaviorSimulationAnalysis.RuleStatistics depositDecisionStatistics =
                analysis.getRuleStatistics().get(
                        BehaviorRuleCode.DEPOSIT_CANCEL_AND_SECURITY_BUY
                );
        List<String> row = new ArrayList<>(List.of(
                condition.name(),
                condition.getDescription(),
                String.valueOf(analysis.getTotalSimulationCount()),
                analysis.getOverallRtScoreSummary().getAverage().toPlainString(),
                analysis.getOverallLhScoreSummary().getAverage().toPlainString(),
                analysis.getOverallRpScoreSummary().getAverage().toPlainString(),
                analysis.getRtScoreDiagnostic().getMaximumScoreRate().toPlainString(),
                analysis.getLhScoreDiagnostic().getMaximumScoreRate().toPlainString(),
                analysis.getRpScoreDiagnostic().getMaximumScoreRate().toPlainString(),
                getPersonaRate(analysis, PersonaType.HLH),
                analysis.getPersonaSummary(PersonaType.HLH).getDistributionRate()
                        .subtract(baseline.getPersonaSummary(PersonaType.HLH).getDistributionRate())
                        .toPlainString(),
                String.valueOf(depositDecisionStatistics.getApplicationCount()),
                depositDecisionStatistics.getRtTotalContribution().toPlainString(),
                depositDecisionStatistics.getLhTotalContribution().toPlainString(),
                depositDecisionStatistics.getRpTotalContribution().toPlainString()
        ));
        for (PersonaType personaType : PersonaType.values()) {
            row.add(getPersonaRate(analysis, personaType));
        }
        return row;
    }

    private String getPersonaRate(
            GameBehaviorSimulationAnalysis analysis,
            PersonaType personaType) {
        return analysis.getPersonaSummary(personaType).getDistributionRate().toPlainString();
    }

    private void writeCsvRow(
            BufferedWriter writer,
            List<String> values) throws IOException {
        writer.write(values.stream()
                .map(this::escapeCsvValue)
                .collect(Collectors.joining(",")));
        writer.newLine();
    }

    private String escapeCsvValue(String value) {
        if (!value.contains(",")
                && !value.contains("\"")
                && !value.contains("\n")
                && !value.contains("\r")) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
