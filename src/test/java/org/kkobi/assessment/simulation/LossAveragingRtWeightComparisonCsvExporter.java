package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.BehaviorRuleCode;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LossAveragingRtWeightComparisonCsvExporter {

    public static final String COMPARISON_FILE_NAME =
            "loss-averaging-rt-weight-comparison.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';
    private static final int CALCULATION_SCALE = 4;

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public void exportLossAveragingRtWeightComparison(
            ScenarioDto scenario,
            int simulationCountPerCondition,
            long randomSeed,
            Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("물타기 RT 가중치 비교 CSV 출력 경로는 필수입니다.");
        }

        try {
            Files.createDirectories(outputDirectory);
            try (BufferedWriter writer = Files.newBufferedWriter(
                    outputDirectory.resolve(COMPARISON_FILE_NAME),
                    StandardCharsets.UTF_8
            )) {
                writer.write(UTF_8_BYTE_ORDER_MARK);
                writeCsvRow(writer, createHeader());
                for (GameBehaviorFrequencyCondition frequencyCondition
                        : GameBehaviorFrequencyCondition.values()) {
                    Map<LossAveragingRtWeightCondition, GameBehaviorSimulationAnalysis> analyses =
                            analyzeWeightConditions(
                                    scenario,
                                    simulationCountPerCondition,
                                    randomSeed,
                                    frequencyCondition
                            );
                    GameBehaviorSimulationAnalysis baselineAnalysis = analyses.get(
                            LossAveragingRtWeightCondition.RT_15
                    );
                    for (LossAveragingRtWeightCondition weightCondition
                            : LossAveragingRtWeightCondition.values()) {
                        writeCsvRow(
                                writer,
                                createComparisonRow(
                                        frequencyCondition,
                                        weightCondition,
                                        analyses.get(weightCondition),
                                        baselineAnalysis
                                )
                        );
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("물타기 RT 가중치 비교 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private Map<LossAveragingRtWeightCondition, GameBehaviorSimulationAnalysis>
            analyzeWeightConditions(
                    ScenarioDto scenario,
                    int simulationCountPerCondition,
                    long randomSeed,
                    GameBehaviorFrequencyCondition frequencyCondition) {
        EnumMap<LossAveragingRtWeightCondition, GameBehaviorSimulationAnalysis> analyses =
                new EnumMap<>(LossAveragingRtWeightCondition.class);
        for (LossAveragingRtWeightCondition weightCondition
                : LossAveragingRtWeightCondition.values()) {
            analyses.put(
                    weightCondition,
                    simulationAnalyzer.analyzeGameSimulations(
                            scenario,
                            simulationCountPerCondition,
                            randomSeed,
                            frequencyCondition,
                            ConsecutiveActionMultiplierCondition.ENABLED,
                            SameTickRuleApplicationCondition.REPEATED,
                            RuleAccumulationCondition.UNLIMITED,
                            weightCondition
                    )
            );
        }
        return analyses;
    }

    private List<String> createHeader() {
        List<String> header = new ArrayList<>(List.of(
                "빈도_조건_코드(frequency_condition)",
                "빈도_조건_설명(frequency_name)",
                "물타기_RT_조건(weight_condition)",
                "물타기_RT_가중치(loss_averaging_rt_weight)",
                "사용자수(simulation_count)",
                "물타기_적용수(loss_averaging_application_count)",
                "물타기_RT_총기여도(loss_averaging_rt_total_contribution)",
                "물타기_RT_사용자당_기여도(loss_averaging_rt_per_user)",
                "기존대비_물타기_RT_총기여도_차이(loss_averaging_rt_difference)",
                "전체_RT_평균(overall_rt_average)",
                "기존대비_RT_평균_차이(overall_rt_average_difference)",
                "전체_RT_표준편차(overall_rt_standard_deviation)",
                "RT_100점_도달률(rt_maximum_score_rate)",
                "기존대비_RT_100점_도달률_차이(rt_maximum_score_rate_difference)",
                "RT_50점_근처_비율(rt_boundary_score_rate)",
                "HLH_판정_비율(hlh_distribution_rate)",
                "기존대비_HLH_비율_차이(hlh_distribution_rate_difference)"
        ));
        for (PersonaType personaType : PersonaType.values()) {
            if (personaType == PersonaType.HLH) {
                continue;
            }
            header.add(personaType.name() + "_판정_비율");
        }
        return header;
    }

    private List<String> createComparisonRow(
            GameBehaviorFrequencyCondition frequencyCondition,
            LossAveragingRtWeightCondition weightCondition,
            GameBehaviorSimulationAnalysis analysis,
            GameBehaviorSimulationAnalysis baselineAnalysis) {
        GameBehaviorSimulationAnalysis.RuleStatistics ruleStatistics =
                analysis.getRuleStatistics().get(BehaviorRuleCode.LOSS_AVERAGING_BUY);
        GameBehaviorSimulationAnalysis.RuleStatistics baselineRuleStatistics =
                baselineAnalysis.getRuleStatistics().get(BehaviorRuleCode.LOSS_AVERAGING_BUY);
        BigDecimal rtAverage = analysis.getOverallRtScoreSummary().getAverage();
        BigDecimal baselineRtAverage = baselineAnalysis.getOverallRtScoreSummary().getAverage();
        BigDecimal rtMaximumRate = analysis.getRtScoreDiagnostic().getMaximumScoreRate();
        BigDecimal baselineRtMaximumRate = baselineAnalysis.getRtScoreDiagnostic()
                .getMaximumScoreRate();
        BigDecimal hlhRate = getPersonaRate(analysis, PersonaType.HLH);
        BigDecimal baselineHlhRate = getPersonaRate(baselineAnalysis, PersonaType.HLH);

        List<String> row = new ArrayList<>(List.of(
                frequencyCondition.name(),
                frequencyCondition.getDescription(),
                weightCondition.name(),
                String.valueOf(weightCondition.getRtWeight()),
                String.valueOf(analysis.getTotalSimulationCount()),
                String.valueOf(ruleStatistics.getApplicationCount()),
                toPlainString(ruleStatistics.getRtTotalContribution()),
                toPlainString(divide(
                        ruleStatistics.getRtTotalContribution(),
                        analysis.getTotalSimulationCount()
                )),
                toPlainString(ruleStatistics.getRtTotalContribution().subtract(
                        baselineRuleStatistics.getRtTotalContribution()
                )),
                toPlainString(rtAverage),
                toPlainString(rtAverage.subtract(baselineRtAverage)),
                toPlainString(analysis.getOverallRtScoreSummary().getStandardDeviation()),
                toPlainString(rtMaximumRate),
                toPlainString(rtMaximumRate.subtract(baselineRtMaximumRate)),
                toPlainString(analysis.getRtScoreDiagnostic().getBoundaryScoreRate()),
                toPlainString(hlhRate),
                toPlainString(hlhRate.subtract(baselineHlhRate))
        ));
        for (PersonaType personaType : PersonaType.values()) {
            if (personaType == PersonaType.HLH) {
                continue;
            }
            row.add(toPlainString(getPersonaRate(analysis, personaType)));
        }
        return row;
    }

    private BigDecimal getPersonaRate(
            GameBehaviorSimulationAnalysis analysis,
            PersonaType personaType) {
        return analysis.getPersonaSummary(personaType).getDistributionRate();
    }

    private BigDecimal divide(BigDecimal value, long divisor) {
        return value.divide(
                BigDecimal.valueOf(divisor),
                CALCULATION_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private String toPlainString(BigDecimal value) {
        return value.toPlainString();
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
