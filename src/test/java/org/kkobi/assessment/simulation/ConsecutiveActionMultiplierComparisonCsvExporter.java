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

public class ConsecutiveActionMultiplierComparisonCsvExporter {

    public static final String COMPARISON_FILE_NAME =
            "consecutive-multiplier-comparison.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public void exportMultiplierComparison(
            ScenarioDto scenario,
            int simulationCountPerCondition,
            long randomSeed,
            Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("연속 행동 배율 비교 CSV 출력 경로는 필수입니다.");
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
                    Map<ConsecutiveActionMultiplierCondition, GameBehaviorSimulationAnalysis>
                            analyses = analyzeMultiplierConditions(
                            scenario,
                            simulationCountPerCondition,
                            randomSeed,
                            frequencyCondition
                    );
                    GameBehaviorSimulationAnalysis enabledAnalysis = analyses.get(
                            ConsecutiveActionMultiplierCondition.ENABLED
                    );
                    for (ConsecutiveActionMultiplierCondition multiplierCondition
                            : ConsecutiveActionMultiplierCondition.values()) {
                        writeCsvRow(
                                writer,
                                createComparisonRow(
                                        frequencyCondition,
                                        multiplierCondition,
                                        analyses.get(multiplierCondition),
                                        enabledAnalysis
                                )
                        );
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("연속 행동 배율 비교 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private Map<ConsecutiveActionMultiplierCondition, GameBehaviorSimulationAnalysis>
            analyzeMultiplierConditions(
                    ScenarioDto scenario,
                    int simulationCountPerCondition,
                    long randomSeed,
                    GameBehaviorFrequencyCondition frequencyCondition) {
        EnumMap<ConsecutiveActionMultiplierCondition, GameBehaviorSimulationAnalysis> analyses =
                new EnumMap<>(ConsecutiveActionMultiplierCondition.class);
        for (ConsecutiveActionMultiplierCondition multiplierCondition
                : ConsecutiveActionMultiplierCondition.values()) {
            analyses.put(
                    multiplierCondition,
                    simulationAnalyzer.analyzeGameSimulations(
                            scenario,
                            simulationCountPerCondition,
                            randomSeed,
                            frequencyCondition,
                            multiplierCondition
                    )
            );
        }
        return analyses;
    }

    private List<String> createHeader() {
        List<String> header = new ArrayList<>(List.of(
                "빈도_조건_코드(frequency_condition)",
                "빈도_조건_설명(frequency_name)",
                "연속_배율_조건(multiplier_condition)",
                "연속_배율_설명(multiplier_name)",
                "연속_배율_기준(multiplier_criteria)",
                "사용자수(simulation_count)",
                "Tick당_평균_행동수(average_action_count_per_tick)",
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
            ConsecutiveActionMultiplierCondition multiplierCondition,
            GameBehaviorSimulationAnalysis analysis,
            GameBehaviorSimulationAnalysis enabledAnalysis) {
        BigDecimal hlhRate = getPersonaRate(analysis, PersonaType.HLH);
        BigDecimal enabledHlhRate = getPersonaRate(enabledAnalysis, PersonaType.HLH);
        BigDecimal rtMaximumRate = analysis.getRtScoreDiagnostic().getMaximumScoreRate();
        BigDecimal enabledRtMaximumRate = enabledAnalysis.getRtScoreDiagnostic()
                .getMaximumScoreRate();
        BigDecimal rpMaximumRate = analysis.getRpScoreDiagnostic().getMaximumScoreRate();
        BigDecimal enabledRpMaximumRate = enabledAnalysis.getRpScoreDiagnostic()
                .getMaximumScoreRate();

        List<String> row = new ArrayList<>(List.of(
                frequencyCondition.name(),
                frequencyCondition.getDescription(),
                multiplierCondition.name(),
                multiplierCondition.getDescription(),
                multiplierCondition.getCriteria(),
                String.valueOf(analysis.getTotalSimulationCount()),
                toPlainString(analysis.getBehaviorStatistics().getAverageActionCountPerTick()),
                toPlainString(hlhRate),
                toPlainString(hlhRate.subtract(enabledHlhRate)),
                toPlainString(rtMaximumRate),
                toPlainString(rtMaximumRate.subtract(enabledRtMaximumRate)),
                toPlainString(rpMaximumRate),
                toPlainString(rpMaximumRate.subtract(enabledRpMaximumRate))
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
