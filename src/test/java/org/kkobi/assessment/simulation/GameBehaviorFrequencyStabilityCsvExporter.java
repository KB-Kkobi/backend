package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.dto.ScenarioDto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameBehaviorFrequencyStabilityCsvExporter {

    public static final String SEED_DETAIL_FILE_NAME = "frequency-seed-detail.csv";
    public static final String SEED_SUMMARY_FILE_NAME = "frequency-seed-summary.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';
    private static final int STATISTICS_SCALE = 4;
    private static final MathContext STATISTICS_CONTEXT =
            new MathContext(20, RoundingMode.HALF_UP);

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public void exportFrequencyStability(
            ScenarioDto scenario,
            int simulationCountPerCondition,
            List<Long> randomSeeds,
            Path outputDirectory) {
        validateInput(simulationCountPerCondition, randomSeeds, outputDirectory);

        try {
            Files.createDirectories(outputDirectory);
            EnumMap<GameBehaviorFrequencyCondition, ConditionStatistics> conditionStatistics =
                    createConditionStatistics();
            try (BufferedWriter detailWriter = createWriter(
                    outputDirectory.resolve(SEED_DETAIL_FILE_NAME)
            )) {
                writeCsvRow(detailWriter, createDetailHeader());
                for (long randomSeed : randomSeeds) {
                    for (GameBehaviorFrequencyCondition condition
                            : GameBehaviorFrequencyCondition.values()) {
                        GameBehaviorSimulationAnalysis analysis =
                                simulationAnalyzer.analyzeGameSimulations(
                                        scenario,
                                        simulationCountPerCondition,
                                        randomSeed,
                                        condition
                                );
                        conditionStatistics.get(condition).addAnalysis(analysis);
                        writeCsvRow(
                                detailWriter,
                                createDetailRow(randomSeed, condition, analysis)
                        );
                    }
                }
            }
            writeSummary(
                    outputDirectory.resolve(SEED_SUMMARY_FILE_NAME),
                    randomSeeds.size(),
                    simulationCountPerCondition,
                    conditionStatistics
            );
        } catch (IOException exception) {
            throw new IllegalStateException("빈도 조건 Seed 안정성 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private EnumMap<GameBehaviorFrequencyCondition, ConditionStatistics>
            createConditionStatistics() {
        EnumMap<GameBehaviorFrequencyCondition, ConditionStatistics> statistics =
                new EnumMap<>(GameBehaviorFrequencyCondition.class);
        for (GameBehaviorFrequencyCondition condition
                : GameBehaviorFrequencyCondition.values()) {
            statistics.put(condition, new ConditionStatistics());
        }
        return statistics;
    }

    private BufferedWriter createWriter(Path path) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        writer.write(UTF_8_BYTE_ORDER_MARK);
        return writer;
    }

    private List<String> createDetailHeader() {
        List<String> header = new ArrayList<>(List.of(
                "난수_Seed(random_seed)",
                "빈도_조건_코드(frequency_condition)",
                "빈도_조건_설명(frequency_name)",
                "Tick당_평균_행동수(average_action_count_per_tick)",
                "RT_100점_도달_비율",
                "LH_100점_도달_비율",
                "RP_100점_도달_비율"
        ));
        for (PersonaType personaType : PersonaType.values()) {
            header.add(personaType.name() + "_판정_비율");
        }
        return header;
    }

    private List<String> createDetailRow(
            long randomSeed,
            GameBehaviorFrequencyCondition condition,
            GameBehaviorSimulationAnalysis analysis) {
        List<String> row = new ArrayList<>(List.of(
                String.valueOf(randomSeed),
                condition.name(),
                condition.getDescription(),
                toPlainString(analysis.getBehaviorStatistics().getAverageActionCountPerTick()),
                toPlainString(analysis.getRtScoreDiagnostic().getMaximumScoreRate()),
                toPlainString(analysis.getLhScoreDiagnostic().getMaximumScoreRate()),
                toPlainString(analysis.getRpScoreDiagnostic().getMaximumScoreRate())
        ));
        for (PersonaType personaType : PersonaType.values()) {
            row.add(toPlainString(analysis.getPersonaSummary(personaType).getDistributionRate()));
        }
        return row;
    }

    private void writeSummary(
            Path summaryPath,
            int seedCount,
            int simulationCountPerCondition,
            Map<GameBehaviorFrequencyCondition, ConditionStatistics> conditionStatistics)
            throws IOException {
        try (BufferedWriter writer = createWriter(summaryPath)) {
            writeCsvRow(writer, createSummaryHeader());
            for (GameBehaviorFrequencyCondition condition
                    : GameBehaviorFrequencyCondition.values()) {
                writeCsvRow(
                        writer,
                        createSummaryRow(
                                condition,
                                seedCount,
                                simulationCountPerCondition,
                                conditionStatistics.get(condition)
                        )
                );
            }
        }
    }

    private List<String> createSummaryHeader() {
        List<String> header = new ArrayList<>(List.of(
                "빈도_조건_코드(frequency_condition)",
                "빈도_조건_설명(frequency_name)",
                "Seed_개수(seed_count)",
                "Seed당_사용자수(simulation_count_per_seed)",
                "Tick당_행동수_평균",
                "Tick당_행동수_표준편차",
                "RT_100점_비율_평균",
                "RT_100점_비율_표준편차",
                "RP_100점_비율_평균",
                "RP_100점_비율_표준편차"
        ));
        for (PersonaType personaType : PersonaType.values()) {
            header.add(personaType.name() + "_비율_평균");
            header.add(personaType.name() + "_비율_표준편차");
        }
        return header;
    }

    private List<String> createSummaryRow(
            GameBehaviorFrequencyCondition condition,
            int seedCount,
            int simulationCountPerCondition,
            ConditionStatistics statistics) {
        List<String> row = new ArrayList<>(List.of(
                condition.name(),
                condition.getDescription(),
                String.valueOf(seedCount),
                String.valueOf(simulationCountPerCondition),
                toPlainString(calculateAverage(statistics.actionCountPerTickValues)),
                toPlainString(calculateStandardDeviation(statistics.actionCountPerTickValues)),
                toPlainString(calculateAverage(statistics.rtMaximumRateValues)),
                toPlainString(calculateStandardDeviation(statistics.rtMaximumRateValues)),
                toPlainString(calculateAverage(statistics.rpMaximumRateValues)),
                toPlainString(calculateStandardDeviation(statistics.rpMaximumRateValues))
        ));
        for (PersonaType personaType : PersonaType.values()) {
            List<BigDecimal> distributionRates = statistics.personaDistributionRates
                    .get(personaType);
            row.add(toPlainString(calculateAverage(distributionRates)));
            row.add(toPlainString(calculateStandardDeviation(distributionRates)));
        }
        return row;
    }

    private BigDecimal calculateAverage(List<BigDecimal> values) {
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(
                BigDecimal.valueOf(values.size()),
                STATISTICS_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal calculateStandardDeviation(List<BigDecimal> values) {
        BigDecimal average = calculateAverage(values);
        BigDecimal variance = values.stream()
                .map(value -> value.subtract(average))
                .map(difference -> difference.multiply(difference))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                        BigDecimal.valueOf(values.size()),
                        STATISTICS_CONTEXT
                );
        return variance.sqrt(STATISTICS_CONTEXT)
                .setScale(STATISTICS_SCALE, RoundingMode.HALF_UP);
    }

    private void validateInput(
            int simulationCountPerCondition,
            List<Long> randomSeeds,
            Path outputDirectory) {
        if (simulationCountPerCondition <= 0) {
            throw new IllegalArgumentException("조건별 시뮬레이션 수는 0보다 커야 합니다.");
        }
        if (randomSeeds == null || randomSeeds.isEmpty()) {
            throw new IllegalArgumentException("안정성 검증 Seed는 한 개 이상 필요합니다.");
        }
        if (randomSeeds.stream().distinct().count() != randomSeeds.size()) {
            throw new IllegalArgumentException("안정성 검증 Seed는 중복될 수 없습니다.");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("안정성 검증 CSV 출력 경로는 필수입니다.");
        }
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

    private static class ConditionStatistics {

        private final List<BigDecimal> actionCountPerTickValues = new ArrayList<>();
        private final List<BigDecimal> rtMaximumRateValues = new ArrayList<>();
        private final List<BigDecimal> rpMaximumRateValues = new ArrayList<>();
        private final EnumMap<PersonaType, List<BigDecimal>> personaDistributionRates =
                createPersonaDistributionRates();

        private void addAnalysis(GameBehaviorSimulationAnalysis analysis) {
            actionCountPerTickValues.add(
                    analysis.getBehaviorStatistics().getAverageActionCountPerTick()
            );
            rtMaximumRateValues.add(analysis.getRtScoreDiagnostic().getMaximumScoreRate());
            rpMaximumRateValues.add(analysis.getRpScoreDiagnostic().getMaximumScoreRate());
            for (PersonaType personaType : PersonaType.values()) {
                personaDistributionRates.get(personaType).add(
                        analysis.getPersonaSummary(personaType).getDistributionRate()
                );
            }
        }

        private static EnumMap<PersonaType, List<BigDecimal>> createPersonaDistributionRates() {
            EnumMap<PersonaType, List<BigDecimal>> rates =
                    new EnumMap<>(PersonaType.class);
            for (PersonaType personaType : PersonaType.values()) {
                rates.put(personaType, new ArrayList<>());
            }
            return rates;
        }
    }
}
