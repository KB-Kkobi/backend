package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.dto.ScenarioDto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SmallTradeDeadZoneFrequencyComparisonCsvExporter {

    public static final String FILE_NAME =
            "small-trade-dead-zone-frequency-comparison.csv";

    private static final List<GameBiasMitigationCondition> COMPARISON_CONDITIONS = List.of(
            GameBiasMitigationCondition.SIZE_SEPARATED_BULL_BUY_ONCE_AND_CAPPED,
            GameBiasMitigationCondition.SMALL_TRADE_DEAD_ZONE_ONCE_AND_CAPPED
    );
    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public Map<ComparisonKey, GameBehaviorSimulationAnalysis> exportComparison(
            ScenarioDto scenario,
            int simulationCountPerCondition,
            long randomSeed,
            Path outputDirectory) {
        if (simulationCountPerCondition <= 0) {
            throw new IllegalArgumentException("조건별 사용자 수는 0보다 커야 합니다.");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("CSV 출력 경로는 필수입니다.");
        }

        try {
            Files.createDirectories(outputDirectory);
            Map<ComparisonKey, GameBehaviorSimulationAnalysis> analyses = new LinkedHashMap<>();
            Map<ComparisonKey, ScoreAxisCorrelationAnalysis> correlations = new LinkedHashMap<>();

            for (GameBiasMitigationCondition mitigationCondition : COMPARISON_CONDITIONS) {
                for (GameBehaviorFrequencyCondition frequencyCondition
                        : GameBehaviorFrequencyCondition.values()) {
                    ComparisonKey comparisonKey = new ComparisonKey(
                            mitigationCondition,
                            frequencyCondition
                    );
                    ScoreAxisCorrelationAnalysis correlationAnalysis =
                            new ScoreAxisCorrelationAnalysis();
                    GameBehaviorSimulationAnalysis analysis =
                            simulationAnalyzer.analyzeGameSimulations(
                                    scenario,
                                    simulationCountPerCondition,
                                    randomSeed,
                                    frequencyCondition,
                                    ConsecutiveActionMultiplierCondition.ENABLED,
                                    mitigationCondition.getSameTickRuleCondition(),
                                    mitigationCondition.getRuleAccumulationCondition(),
                                    LossAveragingRtWeightCondition.RT_15,
                                    mitigationCondition.getRuleEvaluationCondition(),
                                    TradeQuantityGenerationCondition.SYMMETRIC_THREE_LEVEL,
                                    correlationAnalysis::addResult
                            );
                    analyses.put(comparisonKey, analysis);
                    correlations.put(comparisonKey, correlationAnalysis);
                }
            }

            writeComparison(outputDirectory.resolve(FILE_NAME), analyses, correlations);
            return Map.copyOf(analyses);
        } catch (IOException exception) {
            throw new IllegalStateException("소규모 거래 제외 비교 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private void writeComparison(
            Path outputPath,
            Map<ComparisonKey, GameBehaviorSimulationAnalysis> analyses,
            Map<ComparisonKey, ScoreAxisCorrelationAnalysis> correlations) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputPath,
                StandardCharsets.UTF_8
        )) {
            writer.write(UTF_8_BYTE_ORDER_MARK);
            writeCsvRow(writer, createHeader());
            for (GameBiasMitigationCondition mitigationCondition : COMPARISON_CONDITIONS) {
                for (GameBehaviorFrequencyCondition frequencyCondition
                        : GameBehaviorFrequencyCondition.values()) {
                    ComparisonKey comparisonKey = new ComparisonKey(
                            mitigationCondition,
                            frequencyCondition
                    );
                    writeCsvRow(writer, createRow(
                            comparisonKey,
                            analyses.get(comparisonKey),
                            correlations.get(comparisonKey)
                    ));
                }
            }
        }
    }

    private List<String> createHeader() {
        List<String> header = new ArrayList<>(List.of(
                "보정_조건(mitigation_condition)",
                "보정_설명(mitigation_description)",
                "소규모_거래_점수_제외(small_trade_dead_zone)",
                "빈도_조건(frequency_condition)",
                "빈도_설명(frequency_name)",
                "검증_구분(validation_type)",
                "사용자수(simulation_count)",
                "평균_매수_횟수(average_buy_count)",
                "평균_매도_횟수(average_sell_count)",
                "평균_무행동_Tick수(average_no_action_tick_count)",
                "Tick당_평균_행동수(average_action_count_per_tick)",
                "RT_평균(rt_average)",
                "LH_평균(lh_average)",
                "RP_평균(rp_average)",
                "RT_RP_상관계수(rt_rp_correlation)",
                "RT_LH_상관계수(rt_lh_correlation)",
                "LH_RP_상관계수(lh_rp_correlation)"
        ));
        for (PersonaType personaType : PersonaType.values()) {
            header.add(personaType.name() + "_판정_비율");
        }
        return header;
    }

    private List<String> createRow(
            ComparisonKey comparisonKey,
            GameBehaviorSimulationAnalysis analysis,
            ScoreAxisCorrelationAnalysis correlationAnalysis) {
        GameBehaviorSimulationAnalysis.BehaviorStatistics behaviorStatistics =
                analysis.getBehaviorStatistics();
        List<String> row = new ArrayList<>(List.of(
                comparisonKey.mitigationCondition().name(),
                comparisonKey.mitigationCondition().getDescription(),
                String.valueOf(comparisonKey.mitigationCondition()
                        == GameBiasMitigationCondition.SMALL_TRADE_DEAD_ZONE_ONCE_AND_CAPPED),
                comparisonKey.frequencyCondition().name(),
                comparisonKey.frequencyCondition().getDescription(),
                comparisonKey.frequencyCondition().getValidationType(),
                String.valueOf(analysis.getTotalSimulationCount()),
                behaviorStatistics.getAverageBuyCount().toPlainString(),
                behaviorStatistics.getAverageSellCount().toPlainString(),
                behaviorStatistics.getAverageNoActionTickCount().toPlainString(),
                behaviorStatistics.getAverageActionCountPerTick().toPlainString(),
                analysis.getOverallRtScoreSummary().getAverage().toPlainString(),
                analysis.getOverallLhScoreSummary().getAverage().toPlainString(),
                analysis.getOverallRpScoreSummary().getAverage().toPlainString(),
                correlationAnalysis.getRtRpCorrelation().toPlainString(),
                correlationAnalysis.getRtLhCorrelation().toPlainString(),
                correlationAnalysis.getLhRpCorrelation().toPlainString()
        ));
        for (PersonaType personaType : PersonaType.values()) {
            row.add(analysis.getPersonaSummary(personaType)
                    .getDistributionRate()
                    .toPlainString());
        }
        return row;
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

    public record ComparisonKey(
            GameBiasMitigationCondition mitigationCondition,
            GameBehaviorFrequencyCondition frequencyCondition) {
    }
}
