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

public class GameBehaviorFrequencyComparisonCsvExporter {

    public static final String FREQUENCY_COMPARISON_FILE_NAME = "frequency-comparison.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';

    private final GameBehaviorSimulationCsvExporter simulationCsvExporter =
            new GameBehaviorSimulationCsvExporter();

    public Map<GameBehaviorFrequencyCondition, GameBehaviorSimulationAnalysis>
            exportFrequencyComparison(
                    ScenarioDto scenario,
                    int simulationCountPerCondition,
                    long randomSeed,
                    Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("빈도 비교 CSV 출력 경로는 필수입니다.");
        }

        try {
            Files.createDirectories(outputDirectory);
            EnumMap<GameBehaviorFrequencyCondition, GameBehaviorSimulationAnalysis> analyses =
                    new EnumMap<>(GameBehaviorFrequencyCondition.class);
            for (GameBehaviorFrequencyCondition condition
                    : GameBehaviorFrequencyCondition.values()) {
                GameBehaviorSimulationAnalysis analysis =
                        simulationCsvExporter.exportGameSimulations(
                                scenario,
                                simulationCountPerCondition,
                                randomSeed,
                                outputDirectory.resolve(condition.name().toLowerCase()),
                                condition
                        );
                analyses.put(condition, analysis);
            }
            writeFrequencyComparison(
                    outputDirectory.resolve(FREQUENCY_COMPARISON_FILE_NAME),
                    analyses
            );
            return Map.copyOf(analyses);
        } catch (IOException exception) {
            throw new IllegalStateException("빈도 조건 비교 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private void writeFrequencyComparison(
            Path comparisonPath,
            Map<GameBehaviorFrequencyCondition, GameBehaviorSimulationAnalysis> analyses)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                comparisonPath,
                StandardCharsets.UTF_8
        )) {
            writer.write(UTF_8_BYTE_ORDER_MARK);
            writeCsvRow(writer, createHeader());
            for (GameBehaviorFrequencyCondition condition
                    : GameBehaviorFrequencyCondition.values()) {
                writeCsvRow(writer, createComparisonRow(condition, analyses.get(condition)));
            }
        }
    }

    private List<String> createHeader() {
        List<String> header = new ArrayList<>(List.of(
                "빈도_조건_코드(frequency_condition)",
                "빈도_조건_설명(frequency_name)",
                "검증_구분(validation_type)",
                "행동_시작_확률(action_start_percentage)",
                "추가_행동_확률(additional_action_percentage)",
                "조건별_사용자수(simulation_count)",
                "평균_매수_횟수(average_buy_count)",
                "평균_매도_횟수(average_sell_count)",
                "평균_무행동_Tick수(average_no_action_tick_count)",
                "Tick당_평균_행동수(average_action_count_per_tick)",
                "연속행동_2회_발생수(consecutive_action_level_two_count)",
                "연속행동_3회이상_발생수(consecutive_action_level_three_or_more_count)"
        ));
        for (PersonaType personaType : PersonaType.values()) {
            header.add(personaType.name() + "_판정_비율");
        }
        header.addAll(List.of(
                "RT_100점_도달_비율",
                "LH_100점_도달_비율",
                "RP_100점_도달_비율",
                "RT_45점이상_55점이하_비율",
                "LH_45점이상_55점이하_비율",
                "RP_45점이상_55점이하_비율"
        ));
        return header;
    }

    private List<String> createComparisonRow(
            GameBehaviorFrequencyCondition condition,
            GameBehaviorSimulationAnalysis analysis) {
        GameBehaviorSimulationAnalysis.BehaviorStatistics behaviorStatistics =
                analysis.getBehaviorStatistics();
        List<String> row = new ArrayList<>(List.of(
                condition.name(),
                condition.getDescription(),
                condition.getValidationType(),
                String.valueOf(condition.getActionStartPercentage()),
                String.valueOf(condition.getAdditionalActionPercentage()),
                String.valueOf(analysis.getTotalSimulationCount()),
                toPlainString(behaviorStatistics.getAverageBuyCount()),
                toPlainString(behaviorStatistics.getAverageSellCount()),
                toPlainString(behaviorStatistics.getAverageNoActionTickCount()),
                toPlainString(behaviorStatistics.getAverageActionCountPerTick()),
                String.valueOf(behaviorStatistics.getConsecutiveActionLevelTwoCount()),
                String.valueOf(behaviorStatistics.getConsecutiveActionLevelThreeOrMoreCount())
        ));
        for (PersonaType personaType : PersonaType.values()) {
            row.add(toPlainString(analysis.getPersonaSummary(personaType).getDistributionRate()));
        }
        row.addAll(List.of(
                toPlainString(analysis.getRtScoreDiagnostic().getMaximumScoreRate()),
                toPlainString(analysis.getLhScoreDiagnostic().getMaximumScoreRate()),
                toPlainString(analysis.getRpScoreDiagnostic().getMaximumScoreRate()),
                toPlainString(analysis.getRtScoreDiagnostic().getBoundaryScoreRate()),
                toPlainString(analysis.getLhScoreDiagnostic().getBoundaryScoreRate()),
                toPlainString(analysis.getRpScoreDiagnostic().getBoundaryScoreRate())
        ));
        return row;
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
