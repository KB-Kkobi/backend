package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.dto.ScenarioDto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RuleAccumulationComparisonCsvExporter {

    public static final String COMPARISON_FILE_NAME =
            "rule-accumulation-comparison.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public void exportRuleAccumulationComparison(
            ScenarioDto scenario,
            int simulationCountPerCondition,
            long randomSeed,
            Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("규칙 누적 비교 CSV 출력 경로는 필수입니다.");
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
                    Map<RuleAccumulationCondition, GameBehaviorSimulationAnalysis> analyses =
                            analyzeAccumulationConditions(
                                    scenario,
                                    simulationCountPerCondition,
                                    randomSeed,
                                    frequencyCondition
                            );
                    GameBehaviorSimulationAnalysis unlimitedAnalysis = analyses.get(
                            RuleAccumulationCondition.UNLIMITED
                    );
                    for (RuleAccumulationCondition accumulationCondition
                            : RuleAccumulationCondition.values()) {
                        writeCsvRow(
                                writer,
                                createComparisonRow(
                                        frequencyCondition,
                                        accumulationCondition,
                                        analyses.get(accumulationCondition),
                                        unlimitedAnalysis
                                )
                        );
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("규칙 누적 비교 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private Map<RuleAccumulationCondition, GameBehaviorSimulationAnalysis>
            analyzeAccumulationConditions(
                    ScenarioDto scenario,
                    int simulationCountPerCondition,
                    long randomSeed,
                    GameBehaviorFrequencyCondition frequencyCondition) {
        EnumMap<RuleAccumulationCondition, GameBehaviorSimulationAnalysis> analyses =
                new EnumMap<>(RuleAccumulationCondition.class);
        for (RuleAccumulationCondition accumulationCondition
                : RuleAccumulationCondition.values()) {
            analyses.put(
                    accumulationCondition,
                    simulationAnalyzer.analyzeGameSimulations(
                            scenario,
                            simulationCountPerCondition,
                            randomSeed,
                            frequencyCondition,
                            ConsecutiveActionMultiplierCondition.ENABLED,
                            SameTickRuleApplicationCondition.REPEATED,
                            accumulationCondition
                    )
            );
        }
        return analyses;
    }

    private List<String> createHeader() {
        List<String> header = new ArrayList<>(List.of(
                "빈도_조건_코드(frequency_condition)",
                "빈도_조건_설명(frequency_name)",
                "규칙_누적_조건(accumulation_condition)",
                "규칙_누적_설명(accumulation_name)",
                "규칙_누적_기준(accumulation_criteria)",
                "사용자수(simulation_count)",
                "Tick당_평균_행동수(average_action_count_per_tick)",
                "전체_반영_규칙수(total_applied_rule_count)",
                "기존_대비_반영_규칙수_차이",
                "HLH_판정_비율",
                "기존_대비_HLH_비율_차이",
                "RT_100점_도달_비율",
                "기존_대비_RT_100점_비율_차이",
                "RP_100점_도달_비율",
                "기존_대비_RP_100점_비율_차이"
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
            RuleAccumulationCondition accumulationCondition,
            GameBehaviorSimulationAnalysis analysis,
            GameBehaviorSimulationAnalysis unlimitedAnalysis) {
        long totalAppliedRuleCount = calculateTotalRuleApplicationCount(analysis);
        long unlimitedRuleCount = calculateTotalRuleApplicationCount(unlimitedAnalysis);
        BigDecimal hlhRate = getPersonaRate(analysis, PersonaType.HLH);
        BigDecimal unlimitedHlhRate = getPersonaRate(unlimitedAnalysis, PersonaType.HLH);
        BigDecimal rtMaximumRate = analysis.getRtScoreDiagnostic().getMaximumScoreRate();
        BigDecimal unlimitedRtMaximumRate = unlimitedAnalysis.getRtScoreDiagnostic()
                .getMaximumScoreRate();
        BigDecimal rpMaximumRate = analysis.getRpScoreDiagnostic().getMaximumScoreRate();
        BigDecimal unlimitedRpMaximumRate = unlimitedAnalysis.getRpScoreDiagnostic()
                .getMaximumScoreRate();

        List<String> row = new ArrayList<>(List.of(
                frequencyCondition.name(),
                frequencyCondition.getDescription(),
                accumulationCondition.name(),
                accumulationCondition.getDescription(),
                accumulationCondition.getCriteria(),
                String.valueOf(analysis.getTotalSimulationCount()),
                toPlainString(analysis.getBehaviorStatistics().getAverageActionCountPerTick()),
                String.valueOf(totalAppliedRuleCount),
                String.valueOf(totalAppliedRuleCount - unlimitedRuleCount),
                toPlainString(hlhRate),
                toPlainString(hlhRate.subtract(unlimitedHlhRate)),
                toPlainString(rtMaximumRate),
                toPlainString(rtMaximumRate.subtract(unlimitedRtMaximumRate)),
                toPlainString(rpMaximumRate),
                toPlainString(rpMaximumRate.subtract(unlimitedRpMaximumRate))
        ));
        for (PersonaType personaType : PersonaType.values()) {
            if (personaType == PersonaType.HLH) {
                continue;
            }
            row.add(toPlainString(getPersonaRate(analysis, personaType)));
        }
        return row;
    }

    private long calculateTotalRuleApplicationCount(GameBehaviorSimulationAnalysis analysis) {
        return analysis.getRuleStatistics().values().stream()
                .mapToLong(GameBehaviorSimulationAnalysis.RuleStatistics::getApplicationCount)
                .sum();
    }

    private BigDecimal getPersonaRate(
            GameBehaviorSimulationAnalysis analysis,
            PersonaType personaType) {
        return analysis.getPersonaSummary(personaType).getDistributionRate();
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
