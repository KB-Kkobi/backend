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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TradeQuantityGenerationComparisonCsvExporter {

    public static final String COMPARISON_FILE_NAME =
            "trade-quantity-generation-comparison.csv";
    public static final String RULE_COMPARISON_FILE_NAME =
            "trade-quantity-rule-comparison.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';
    private static final int CALCULATION_SCALE = 4;

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public void exportComparison(
            ScenarioDto scenario,
            int simulationCount,
            long randomSeed,
            Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("거래 수량 생성 비교 CSV 출력 경로는 필수입니다.");
        }

        try {
            Files.createDirectories(outputDirectory);
            EnumMap<TradeQuantityGenerationCondition, GameBehaviorSimulationAnalysis> analyses =
                    analyzeConditions(scenario, simulationCount, randomSeed);
            try (BufferedWriter writer = Files.newBufferedWriter(
                    outputDirectory.resolve(COMPARISON_FILE_NAME),
                    StandardCharsets.UTF_8
            )) {
                writer.write(UTF_8_BYTE_ORDER_MARK);
                writeCsvRow(writer, createHeader());
                for (TradeQuantityGenerationCondition condition
                        : TradeQuantityGenerationCondition.values()) {
                    writeCsvRow(writer, createRow(condition, analyses.get(condition)));
                }
            }
            writeRuleComparison(outputDirectory, analyses);
        } catch (IOException exception) {
            throw new IllegalStateException("거래 수량 생성 비교 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private EnumMap<TradeQuantityGenerationCondition, GameBehaviorSimulationAnalysis>
            analyzeConditions(
                    ScenarioDto scenario,
                    int simulationCount,
                    long randomSeed) {
        EnumMap<TradeQuantityGenerationCondition, GameBehaviorSimulationAnalysis> analyses =
                new EnumMap<>(TradeQuantityGenerationCondition.class);
        for (TradeQuantityGenerationCondition condition
                : TradeQuantityGenerationCondition.values()) {
            analyses.put(condition, analyze(scenario, simulationCount, randomSeed, condition));
        }
        return analyses;
    }

    private GameBehaviorSimulationAnalysis analyze(
            ScenarioDto scenario,
            int simulationCount,
            long randomSeed,
            TradeQuantityGenerationCondition condition) {
        return simulationAnalyzer.analyzeGameSimulations(
                scenario,
                simulationCount,
                randomSeed,
                GameBehaviorFrequencyCondition.MEDIUM,
                ConsecutiveActionMultiplierCondition.ENABLED,
                SameTickRuleApplicationCondition.REPEATED,
                RuleAccumulationCondition.UNLIMITED,
                LossAveragingRtWeightCondition.RT_15,
                GameRuleEvaluationCondition
                        .EXCLUSIVE_MODERATE_FIXED_ACTION_SCORE_AND_DEPOSIT_DECISION,
                condition,
                simulationResult -> {
                }
        );
    }

    private List<String> createHeader() {
        List<String> header = new ArrayList<>(List.of(
                "수량_조건(quantity_condition)",
                "수량_조건_설명(quantity_condition_name)",
                "사용자수(simulation_count)",
                "평균_매수_횟수(average_buy_count)",
                "평균_매도_횟수(average_sell_count)",
                "매수_매도_횟수차이(buy_sell_count_gap)",
                "매수_매도_비율(buy_sell_ratio)",
                "평균_무행동_Tick수(average_no_action_tick_count)",
                "Tick당_평균_행동수(average_action_count_per_tick)",
                "RT_평균(rt_average)",
                "LH_평균(lh_average)",
                "RP_평균(rp_average)",
                "RT_100점_비율(rt_maximum_rate)",
                "LH_100점_비율(lh_maximum_rate)",
                "RP_100점_비율(rp_maximum_rate)"
        ));
        for (PersonaType personaType : PersonaType.values()) {
            header.add(personaType.name() + "_판정_비율");
        }
        return header;
    }

    private void writeRuleComparison(
            Path outputDirectory,
            Map<TradeQuantityGenerationCondition, GameBehaviorSimulationAnalysis> analyses)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputDirectory.resolve(RULE_COMPARISON_FILE_NAME),
                StandardCharsets.UTF_8
        )) {
            writer.write(UTF_8_BYTE_ORDER_MARK);
            writeCsvRow(writer, createRuleHeader());
            for (TradeQuantityGenerationCondition condition
                    : TradeQuantityGenerationCondition.values()) {
                GameBehaviorSimulationAnalysis analysis = analyses.get(condition);
                for (BehaviorRuleCode ruleCode : BehaviorRuleCode.values()) {
                    GameBehaviorSimulationAnalysis.RuleStatistics ruleStatistics =
                            analysis.getRuleStatistics().get(ruleCode);
                    writeCsvRow(
                            writer,
                            createRuleRow(condition, analysis, ruleStatistics)
                    );
                }
            }
        }
    }

    private List<String> createRuleHeader() {
        return List.of(
                "수량_조건(quantity_condition)",
                "규칙_코드(rule_code)",
                "규칙_설명(rule_name)",
                "전체_적용수(application_count)",
                "사용자당_평균_적용수(application_count_per_user)",
                "RT_총기여도(rt_total_contribution)",
                "LH_총기여도(lh_total_contribution)",
                "RP_총기여도(rp_total_contribution)",
                "RT_사용자당_기여도(rt_contribution_per_user)",
                "LH_사용자당_기여도(lh_contribution_per_user)",
                "RP_사용자당_기여도(rp_contribution_per_user)"
        );
    }

    private List<String> createRuleRow(
            TradeQuantityGenerationCondition condition,
            GameBehaviorSimulationAnalysis analysis,
            GameBehaviorSimulationAnalysis.RuleStatistics ruleStatistics) {
        BigDecimal simulationCount = BigDecimal.valueOf(analysis.getTotalSimulationCount());
        return List.of(
                condition.name(),
                ruleStatistics.getRuleCode().name(),
                GameBehaviorSimulationCsvExporter.getRuleName(ruleStatistics.getRuleCode()),
                String.valueOf(ruleStatistics.getApplicationCount()),
                divide(
                        BigDecimal.valueOf(ruleStatistics.getApplicationCount()),
                        simulationCount
                ).toPlainString(),
                ruleStatistics.getRtTotalContribution().toPlainString(),
                ruleStatistics.getLhTotalContribution().toPlainString(),
                ruleStatistics.getRpTotalContribution().toPlainString(),
                divide(ruleStatistics.getRtTotalContribution(), simulationCount).toPlainString(),
                divide(ruleStatistics.getLhTotalContribution(), simulationCount).toPlainString(),
                divide(ruleStatistics.getRpTotalContribution(), simulationCount).toPlainString()
        );
    }

    private List<String> createRow(
            TradeQuantityGenerationCondition condition,
            GameBehaviorSimulationAnalysis analysis) {
        GameBehaviorSimulationAnalysis.BehaviorStatistics behaviorStatistics =
                analysis.getBehaviorStatistics();
        BigDecimal averageBuyCount = behaviorStatistics.getAverageBuyCount();
        BigDecimal averageSellCount = behaviorStatistics.getAverageSellCount();
        List<String> row = new ArrayList<>(List.of(
                condition.name(),
                condition.getDescription(),
                String.valueOf(analysis.getTotalSimulationCount()),
                averageBuyCount.toPlainString(),
                averageSellCount.toPlainString(),
                averageBuyCount.subtract(averageSellCount).toPlainString(),
                divide(averageBuyCount, averageSellCount).toPlainString(),
                behaviorStatistics.getAverageNoActionTickCount().toPlainString(),
                behaviorStatistics.getAverageActionCountPerTick().toPlainString(),
                analysis.getOverallRtScoreSummary().getAverage().toPlainString(),
                analysis.getOverallLhScoreSummary().getAverage().toPlainString(),
                analysis.getOverallRpScoreSummary().getAverage().toPlainString(),
                analysis.getRtScoreDiagnostic().getMaximumScoreRate().toPlainString(),
                analysis.getLhScoreDiagnostic().getMaximumScoreRate().toPlainString(),
                analysis.getRpScoreDiagnostic().getMaximumScoreRate().toPlainString()
        ));
        for (PersonaType personaType : PersonaType.values()) {
            row.add(analysis.getPersonaSummary(personaType)
                    .getDistributionRate()
                    .toPlainString());
        }
        return row;
    }

    private BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(CALCULATION_SCALE);
        }
        return dividend.divide(divisor, CALCULATION_SCALE, RoundingMode.HALF_UP);
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
