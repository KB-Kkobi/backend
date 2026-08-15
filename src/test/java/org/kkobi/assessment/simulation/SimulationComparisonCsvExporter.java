package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.dto.ScenarioDto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
            Map<String, Long> crashHoldingEpisodeTotals = new LinkedHashMap<>();
            Map<String, Long> normalPlannedBuyTotals = new LinkedHashMap<>();
            Map<String, Long> cashBufferMaintenanceTotals = new LinkedHashMap<>();

            for (SimulationExperimentCase experimentCase : experimentCases) {
                ScoreAxisCorrelationAnalysis correlationAnalysis =
                        new ScoreAxisCorrelationAnalysis();
                long[] crashHoldingEpisodeTotal = {0L};
                long[] normalPlannedBuyTotal = {0L};
                long[] cashBufferMaintenanceTotal = {0L};
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
                                result -> {
                                    correlationAnalysis.addResult(result);
                                    crashHoldingEpisodeTotal[0] += result.getCrashHoldingEpisodeCount();
                                    normalPlannedBuyTotal[0] += result.getNormalPlannedBuyCount();
                                    cashBufferMaintenanceTotal[0] +=
                                            result.getCashBufferMaintenanceCount();
                                }
                        );
                if (analyses.putIfAbsent(experimentCase.name(), analysis) != null) {
                    throw new IllegalArgumentException("실험 이름이 중복되었습니다: " + experimentCase.name());
                }
                correlations.put(experimentCase.name(), correlationAnalysis);
                crashHoldingEpisodeTotals.put(
                        experimentCase.name(),
                        crashHoldingEpisodeTotal[0]
                );
                normalPlannedBuyTotals.put(
                        experimentCase.name(),
                        normalPlannedBuyTotal[0]
                );
                cashBufferMaintenanceTotals.put(
                        experimentCase.name(),
                        cashBufferMaintenanceTotal[0]
                );
            }

            writeComparison(
                    outputDirectory.resolve(FILE_NAME),
                    experimentCases,
                    analyses,
                    correlations,
                    crashHoldingEpisodeTotals,
                    normalPlannedBuyTotals,
                    cashBufferMaintenanceTotals
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
            Map<String, ScoreAxisCorrelationAnalysis> correlations,
            Map<String, Long> crashHoldingEpisodeTotals,
            Map<String, Long> normalPlannedBuyTotals,
            Map<String, Long> cashBufferMaintenanceTotals) throws IOException {
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
                        correlations.get(experimentCase.name()),
                        crashHoldingEpisodeTotals.get(experimentCase.name()),
                        normalPlannedBuyTotals.get(experimentCase.name()),
                        cashBufferMaintenanceTotals.get(experimentCase.name())
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
                "평균_급락구간_보유유지_횟수(average_crash_holding_episode_count)",
                "평균_평범장_계획매수_횟수(average_normal_planned_buy_count)",
                "평균_현금완충_유지_횟수(average_cash_buffer_maintenance_count)",
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
            ScoreAxisCorrelationAnalysis correlationAnalysis,
            long crashHoldingEpisodeTotal,
            long normalPlannedBuyTotal,
            long cashBufferMaintenanceTotal) {
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
                BigDecimal.valueOf(crashHoldingEpisodeTotal)
                        .divide(
                                BigDecimal.valueOf(analysis.getTotalSimulationCount()),
                                4,
                                RoundingMode.HALF_UP
                        )
                        .toPlainString(),
                BigDecimal.valueOf(normalPlannedBuyTotal)
                        .divide(
                                BigDecimal.valueOf(analysis.getTotalSimulationCount()),
                                4,
                                RoundingMode.HALF_UP
                        )
                        .toPlainString(),
                BigDecimal.valueOf(cashBufferMaintenanceTotal)
                        .divide(
                                BigDecimal.valueOf(analysis.getTotalSimulationCount()),
                                4,
                                RoundingMode.HALF_UP
                        )
                        .toPlainString(),
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
