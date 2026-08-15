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

public class SimulationComparisonCsvExporter {

    public static final String FILE_NAME = "simulation-comparison.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public Map<String, GameBehaviorSimulationAnalysis> exportComparison(
            ScenarioDto scenario,
            List<SimulationExperimentCase> experimentCases,
            int simulationCountPerCase,
            long randomSeed,
            Path outputDirectory) {
        if (experimentCases == null || experimentCases.isEmpty()) {
            throw new IllegalArgumentException("실험 조건은 한 개 이상 필요합니다.");
        }
        if (simulationCountPerCase <= 0) {
            throw new IllegalArgumentException("조건별 사용자 수는 0보다 커야 합니다.");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("CSV 출력 경로는 필수입니다.");
        }

        try {
            Files.createDirectories(outputDirectory);
            Map<String, GameBehaviorSimulationAnalysis> analyses = new LinkedHashMap<>();
            Map<String, ScoreAxisCorrelationAnalysis> correlations = new LinkedHashMap<>();

            for (SimulationExperimentCase experimentCase : experimentCases) {
                ScoreAxisCorrelationAnalysis correlationAnalysis =
                        new ScoreAxisCorrelationAnalysis();
                GameBiasMitigationCondition mitigationCondition =
                        experimentCase.mitigationCondition();
                GameBehaviorSimulationAnalysis analysis =
                        simulationAnalyzer.analyzeGameSimulations(
                                scenario,
                                simulationCountPerCase,
                                randomSeed,
                                experimentCase.frequencyCondition(),
                                ConsecutiveActionMultiplierCondition.ENABLED,
                                mitigationCondition.getSameTickRuleCondition(),
                                mitigationCondition.getRuleAccumulationCondition(),
                                LossAveragingRtWeightCondition.RT_15,
                                mitigationCondition.getRuleEvaluationCondition(),
                                TradeQuantityGenerationCondition.SYMMETRIC_THREE_LEVEL,
                                correlationAnalysis::addResult
                        );
                if (analyses.putIfAbsent(experimentCase.name(), analysis) != null) {
                    throw new IllegalArgumentException("실험 이름이 중복되었습니다: " + experimentCase.name());
                }
                correlations.put(experimentCase.name(), correlationAnalysis);
            }

            writeComparison(
                    outputDirectory.resolve(FILE_NAME),
                    experimentCases,
                    analyses,
                    correlations
            );
            return Map.copyOf(analyses);
        } catch (IOException exception) {
            throw new IllegalStateException("시뮬레이션 비교 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private void writeComparison(
            Path outputPath,
            List<SimulationExperimentCase> experimentCases,
            Map<String, GameBehaviorSimulationAnalysis> analyses,
            Map<String, ScoreAxisCorrelationAnalysis> correlations) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputPath,
                StandardCharsets.UTF_8
        )) {
            writer.write(UTF_8_BYTE_ORDER_MARK);
            writeCsvRow(writer, createHeader());
            for (SimulationExperimentCase experimentCase : experimentCases) {
                writeCsvRow(writer, createRow(
                        experimentCase,
                        analyses.get(experimentCase.name()),
                        correlations.get(experimentCase.name())
                ));
            }
        }
    }

    private List<String> createHeader() {
        List<String> header = new ArrayList<>(List.of(
                "실험_이름(experiment_name)",
                "실험_설명(experiment_description)",
                "보정_조건(mitigation_condition)",
                "보정_설명(mitigation_description)",
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
            SimulationExperimentCase experimentCase,
            GameBehaviorSimulationAnalysis analysis,
            ScoreAxisCorrelationAnalysis correlationAnalysis) {
        GameBehaviorSimulationAnalysis.BehaviorStatistics behaviorStatistics =
                analysis.getBehaviorStatistics();
        List<String> row = new ArrayList<>(List.of(
                experimentCase.name(),
                experimentCase.description(),
                experimentCase.mitigationCondition().name(),
                experimentCase.mitigationCondition().getDescription(),
                experimentCase.frequencyCondition().name(),
                experimentCase.frequencyCondition().getDescription(),
                experimentCase.frequencyCondition().getValidationType(),
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
