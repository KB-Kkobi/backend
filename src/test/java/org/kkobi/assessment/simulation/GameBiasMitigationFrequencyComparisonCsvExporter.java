package org.kkobi.assessment.simulation;

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

public class GameBiasMitigationFrequencyComparisonCsvExporter {

    public static final String FILE_NAME =
            "game-bias-frequency-comparison.csv";

    private static final GameBiasMitigationCondition TARGET_CONDITION =
            GameBiasMitigationCondition.SIZE_SEPARATED_BULL_BUY_ONCE_AND_CAPPED;
    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public Map<GameBehaviorFrequencyCondition, GameBehaviorSimulationAnalysis> exportComparison(
            ScenarioDto scenario,
            int simulationCountPerCondition,
            long randomSeed,
            Path outputDirectory) {
        if (simulationCountPerCondition <= 0) {
            throw new IllegalArgumentException("빈도 조건별 사용자 수는 0보다 커야 합니다.");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("빈도 비교 CSV 출력 경로는 필수입니다.");
        }

        try {
            Files.createDirectories(outputDirectory);
            EnumMap<GameBehaviorFrequencyCondition, GameBehaviorSimulationAnalysis> analyses =
                    new EnumMap<>(GameBehaviorFrequencyCondition.class);
            EnumMap<GameBehaviorFrequencyCondition, ScoreAxisCorrelationAnalysis> correlations =
                    new EnumMap<>(GameBehaviorFrequencyCondition.class);
            for (GameBehaviorFrequencyCondition frequencyCondition
                    : GameBehaviorFrequencyCondition.values()) {
                ScoreAxisCorrelationAnalysis correlationAnalysis =
                        new ScoreAxisCorrelationAnalysis();
                GameBehaviorSimulationAnalysis analysis =
                        simulationAnalyzer.analyzeGameSimulations(
                                scenario,
                                simulationCountPerCondition,
                                randomSeed,
                                frequencyCondition,
                                ConsecutiveActionMultiplierCondition.ENABLED,
                                TARGET_CONDITION.getSameTickRuleCondition(),
                                TARGET_CONDITION.getRuleAccumulationCondition(),
                                LossAveragingRtWeightCondition.RT_15,
                                TARGET_CONDITION.getRuleEvaluationCondition(),
                                TradeQuantityGenerationCondition.SYMMETRIC_THREE_LEVEL,
                                correlationAnalysis::addResult
                        );
                analyses.put(frequencyCondition, analysis);
                correlations.put(frequencyCondition, correlationAnalysis);
            }
            writeComparison(
                    outputDirectory.resolve(FILE_NAME),
                    analyses,
                    correlations
            );
            return Map.copyOf(analyses);
        } catch (IOException exception) {
            throw new IllegalStateException("편중 완화 규칙의 빈도 비교 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private void writeComparison(
            Path outputPath,
            Map<GameBehaviorFrequencyCondition, GameBehaviorSimulationAnalysis> analyses,
            Map<GameBehaviorFrequencyCondition, ScoreAxisCorrelationAnalysis> correlations)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputPath,
                StandardCharsets.UTF_8
        )) {
            writer.write(UTF_8_BYTE_ORDER_MARK);
            writeCsvRow(writer, createHeader());
            for (GameBehaviorFrequencyCondition frequencyCondition
                    : GameBehaviorFrequencyCondition.values()) {
                writeCsvRow(
                        writer,
                        createRow(
                                frequencyCondition,
                                analyses.get(frequencyCondition),
                                correlations.get(frequencyCondition)
                        )
                );
            }
        }
    }

    private List<String> createHeader() {
        List<String> header = new ArrayList<>(List.of(
                "보정_조건(mitigation_condition)",
                "빈도_조건(frequency_condition)",
                "빈도_설명(frequency_name)",
                "검증_구분(validation_type)",
                "행동_시작_확률(action_start_percentage)",
                "추가_행동_확률(additional_action_percentage)",
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
            GameBehaviorFrequencyCondition frequencyCondition,
            GameBehaviorSimulationAnalysis analysis,
            ScoreAxisCorrelationAnalysis correlationAnalysis) {
        GameBehaviorSimulationAnalysis.BehaviorStatistics behaviorStatistics =
                analysis.getBehaviorStatistics();
        List<String> row = new ArrayList<>(List.of(
                TARGET_CONDITION.name(),
                frequencyCondition.name(),
                frequencyCondition.getDescription(),
                frequencyCondition.getValidationType(),
                String.valueOf(frequencyCondition.getActionStartPercentage()),
                String.valueOf(frequencyCondition.getAdditionalActionPercentage()),
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
}
