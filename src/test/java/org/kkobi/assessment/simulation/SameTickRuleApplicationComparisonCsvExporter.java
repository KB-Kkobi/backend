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

public class SameTickRuleApplicationComparisonCsvExporter {

    public static final String COMPARISON_FILE_NAME =
            "same-tick-rule-comparison.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public void exportSameTickRuleComparison(
            ScenarioDto scenario,
            int simulationCountPerCondition,
            long randomSeed,
            Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("동일 Tick 규칙 비교 CSV 출력 경로는 필수입니다.");
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
                    Map<SameTickRuleApplicationCondition, GameBehaviorSimulationAnalysis>
                            analyses = analyzeSameTickConditions(
                            scenario,
                            simulationCountPerCondition,
                            randomSeed,
                            frequencyCondition
                    );
                    GameBehaviorSimulationAnalysis repeatedAnalysis = analyses.get(
                            SameTickRuleApplicationCondition.REPEATED
                    );
                    for (SameTickRuleApplicationCondition sameTickRuleCondition
                            : SameTickRuleApplicationCondition.values()) {
                        writeCsvRow(
                                writer,
                                createComparisonRow(
                                        frequencyCondition,
                                        sameTickRuleCondition,
                                        analyses.get(sameTickRuleCondition),
                                        repeatedAnalysis
                                )
                        );
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("동일 Tick 규칙 비교 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private Map<SameTickRuleApplicationCondition, GameBehaviorSimulationAnalysis>
            analyzeSameTickConditions(
                    ScenarioDto scenario,
                    int simulationCountPerCondition,
                    long randomSeed,
                    GameBehaviorFrequencyCondition frequencyCondition) {
        EnumMap<SameTickRuleApplicationCondition, GameBehaviorSimulationAnalysis> analyses =
                new EnumMap<>(SameTickRuleApplicationCondition.class);
        for (SameTickRuleApplicationCondition sameTickRuleCondition
                : SameTickRuleApplicationCondition.values()) {
            analyses.put(
                    sameTickRuleCondition,
                    simulationAnalyzer.analyzeGameSimulations(
                            scenario,
                            simulationCountPerCondition,
                            randomSeed,
                            frequencyCondition,
                            ConsecutiveActionMultiplierCondition.ENABLED,
                            sameTickRuleCondition
                    )
            );
        }
        return analyses;
    }

    private List<String> createHeader() {
        List<String> header = new ArrayList<>(List.of(
                "빈도_조건_코드(frequency_condition)",
                "빈도_조건_설명(frequency_name)",
                "동일_Tick_규칙_조건(same_tick_rule_condition)",
                "동일_Tick_규칙_설명(same_tick_rule_name)",
                "동일_Tick_규칙_기준(same_tick_rule_criteria)",
                "사용자수(simulation_count)",
                "Tick당_평균_행동수(average_action_count_per_tick)",
                "전체_규칙_적용수(total_rule_application_count)",
                "기존_대비_규칙_적용수_차이",
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
            SameTickRuleApplicationCondition sameTickRuleCondition,
            GameBehaviorSimulationAnalysis analysis,
            GameBehaviorSimulationAnalysis repeatedAnalysis) {
        long totalRuleApplicationCount = calculateTotalRuleApplicationCount(analysis);
        long repeatedRuleApplicationCount = calculateTotalRuleApplicationCount(repeatedAnalysis);
        BigDecimal hlhRate = getPersonaRate(analysis, PersonaType.HLH);
        BigDecimal repeatedHlhRate = getPersonaRate(repeatedAnalysis, PersonaType.HLH);
        BigDecimal rtMaximumRate = analysis.getRtScoreDiagnostic().getMaximumScoreRate();
        BigDecimal repeatedRtMaximumRate = repeatedAnalysis.getRtScoreDiagnostic()
                .getMaximumScoreRate();
        BigDecimal rpMaximumRate = analysis.getRpScoreDiagnostic().getMaximumScoreRate();
        BigDecimal repeatedRpMaximumRate = repeatedAnalysis.getRpScoreDiagnostic()
                .getMaximumScoreRate();

        List<String> row = new ArrayList<>(List.of(
                frequencyCondition.name(),
                frequencyCondition.getDescription(),
                sameTickRuleCondition.name(),
                sameTickRuleCondition.getDescription(),
                sameTickRuleCondition.getCriteria(),
                String.valueOf(analysis.getTotalSimulationCount()),
                toPlainString(analysis.getBehaviorStatistics().getAverageActionCountPerTick()),
                String.valueOf(totalRuleApplicationCount),
                String.valueOf(totalRuleApplicationCount - repeatedRuleApplicationCount),
                toPlainString(hlhRate),
                toPlainString(hlhRate.subtract(repeatedHlhRate)),
                toPlainString(rtMaximumRate),
                toPlainString(rtMaximumRate.subtract(repeatedRtMaximumRate)),
                toPlainString(rpMaximumRate),
                toPlainString(rpMaximumRate.subtract(repeatedRpMaximumRate))
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
