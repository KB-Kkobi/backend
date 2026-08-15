package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.dto.ScenarioDto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameBiasMitigationComparisonCsvExporter {

    public static final String COMPARISON_FILE_NAME =
            "game-bias-mitigation-comparison.csv";
    public static final String RULE_COMPARISON_FILE_NAME =
            "game-bias-mitigation-rule-comparison.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public void exportComparison(
            ScenarioDto scenario,
            int simulationCount,
            long randomSeed,
            Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("편향 완화 비교 CSV 출력 경로는 필수입니다.");
        }

        try {
            Files.createDirectories(outputDirectory);
            EnumMap<GameBiasMitigationCondition, GameBehaviorSimulationAnalysis> analyses =
                    analyzeConditions(scenario, simulationCount, randomSeed);
            writeSummary(outputDirectory, analyses);
            writeRuleComparison(outputDirectory, analyses);
        } catch (IOException exception) {
            throw new IllegalStateException("편향 완화 비교 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private EnumMap<GameBiasMitigationCondition, GameBehaviorSimulationAnalysis>
            analyzeConditions(
                    ScenarioDto scenario,
                    int simulationCount,
                    long randomSeed) {
        EnumMap<GameBiasMitigationCondition, GameBehaviorSimulationAnalysis> analyses =
                new EnumMap<>(GameBiasMitigationCondition.class);
        for (GameBiasMitigationCondition condition : GameBiasMitigationCondition.values()) {
            analyses.put(
                    condition,
                    simulationAnalyzer.analyzeGameSimulations(
                            scenario,
                            simulationCount,
                            randomSeed,
                            GameBehaviorFrequencyCondition.MEDIUM,
                            ConsecutiveActionMultiplierCondition.ENABLED,
                            condition.getSameTickRuleCondition(),
                            condition.getRuleAccumulationCondition(),
                            LossAveragingRtWeightCondition.RT_15,
                            condition.getRuleEvaluationCondition(),
                            TradeQuantityGenerationCondition.SYMMETRIC_THREE_LEVEL,
                            simulationResult -> {
                            }
                    )
            );
        }
        return analyses;
    }

    private void writeSummary(
            Path outputDirectory,
            Map<GameBiasMitigationCondition, GameBehaviorSimulationAnalysis> analyses)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputDirectory.resolve(COMPARISON_FILE_NAME),
                StandardCharsets.UTF_8
        )) {
            writer.write(UTF_8_BYTE_ORDER_MARK);
            writeCsvRow(writer, createSummaryHeader());
            for (GameBiasMitigationCondition condition : GameBiasMitigationCondition.values()) {
                writeCsvRow(writer, createSummaryRow(condition, analyses.get(condition)));
            }
        }
    }

    private List<String> createSummaryHeader() {
        List<String> header = new ArrayList<>(List.of(
                "보정_조건(mitigation_condition)",
                "보정_조건_설명(mitigation_name)",
                "사용자수(simulation_count)",
                "평균_매수_횟수(average_buy_count)",
                "평균_매도_횟수(average_sell_count)",
                "RT_평균(rt_average)",
                "LH_평균(lh_average)",
                "RP_평균(rp_average)",
                "RT_100점_비율(rt_maximum_rate)",
                "LH_100점_비율(lh_maximum_rate)",
                "RP_100점_비율(rp_maximum_rate)",
                "전체_규칙_반영수(total_rule_application_count)"
        ));
        for (PersonaType personaType : PersonaType.values()) {
            header.add(personaType.name() + "_판정_비율");
        }
        return header;
    }

    private List<String> createSummaryRow(
            GameBiasMitigationCondition condition,
            GameBehaviorSimulationAnalysis analysis) {
        List<String> row = new ArrayList<>(List.of(
                condition.name(),
                condition.getDescription(),
                String.valueOf(analysis.getTotalSimulationCount()),
                analysis.getBehaviorStatistics().getAverageBuyCount().toPlainString(),
                analysis.getBehaviorStatistics().getAverageSellCount().toPlainString(),
                analysis.getOverallRtScoreSummary().getAverage().toPlainString(),
                analysis.getOverallLhScoreSummary().getAverage().toPlainString(),
                analysis.getOverallRpScoreSummary().getAverage().toPlainString(),
                analysis.getRtScoreDiagnostic().getMaximumScoreRate().toPlainString(),
                analysis.getLhScoreDiagnostic().getMaximumScoreRate().toPlainString(),
                analysis.getRpScoreDiagnostic().getMaximumScoreRate().toPlainString(),
                String.valueOf(calculateTotalRuleApplicationCount(analysis))
        ));
        for (PersonaType personaType : PersonaType.values()) {
            row.add(analysis.getPersonaSummary(personaType)
                    .getDistributionRate()
                    .toPlainString());
        }
        return row;
    }

    private long calculateTotalRuleApplicationCount(GameBehaviorSimulationAnalysis analysis) {
        return analysis.getRuleStatistics().values().stream()
                .mapToLong(GameBehaviorSimulationAnalysis.RuleStatistics::getApplicationCount)
                .sum();
    }

    private void writeRuleComparison(
            Path outputDirectory,
            Map<GameBiasMitigationCondition, GameBehaviorSimulationAnalysis> analyses)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputDirectory.resolve(RULE_COMPARISON_FILE_NAME),
                StandardCharsets.UTF_8
        )) {
            writer.write(UTF_8_BYTE_ORDER_MARK);
            writeCsvRow(writer, List.of(
                    "보정_조건(mitigation_condition)",
                    "규칙_코드(rule_code)",
                    "규칙_설명(rule_name)",
                    "전체_적용수(application_count)",
                    "RT_총기여도(rt_total_contribution)",
                    "LH_총기여도(lh_total_contribution)",
                    "RP_총기여도(rp_total_contribution)"
            ));
            for (GameBiasMitigationCondition condition : GameBiasMitigationCondition.values()) {
                GameBehaviorSimulationAnalysis analysis = analyses.get(condition);
                for (BehaviorRuleCode ruleCode : BehaviorRuleCode.values()) {
                    GameBehaviorSimulationAnalysis.RuleStatistics ruleStatistics =
                            analysis.getRuleStatistics().get(ruleCode);
                    writeCsvRow(writer, List.of(
                            condition.name(),
                            ruleCode.name(),
                            GameBehaviorSimulationCsvExporter.getRuleName(ruleCode),
                            String.valueOf(ruleStatistics.getApplicationCount()),
                            ruleStatistics.getRtTotalContribution().toPlainString(),
                            ruleStatistics.getLhTotalContribution().toPlainString(),
                            ruleStatistics.getRpTotalContribution().toPlainString()
                    ));
                }
            }
        }
    }

    private void writeCsvRow(BufferedWriter writer, List<String> values) throws IOException {
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
