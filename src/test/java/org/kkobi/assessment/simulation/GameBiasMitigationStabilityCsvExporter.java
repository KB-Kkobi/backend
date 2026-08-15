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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GameBiasMitigationStabilityCsvExporter {

    public static final String SEED_DETAIL_FILE_NAME =
            "game-bias-seed-detail.csv";
    public static final String PERSONA_STABILITY_FILE_NAME =
            "game-bias-persona-stability.csv";
    public static final String AXIS_CORRELATION_FILE_NAME =
            "game-bias-axis-correlation.csv";

    private static final GameBiasMitigationCondition TARGET_CONDITION =
            GameBiasMitigationCondition.SIZE_SEPARATED_BULL_BUY_ONCE_AND_CAPPED;
    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';
    private static final int STATISTICS_SCALE = 4;
    private static final MathContext STATISTICS_CONTEXT =
            new MathContext(20, RoundingMode.HALF_UP);

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public void exportStability(
            ScenarioDto scenario,
            int simulationCount,
            List<Long> randomSeeds,
            Path outputDirectory) {
        validateInput(simulationCount, randomSeeds, outputDirectory);

        try {
            Files.createDirectories(outputDirectory);
            ScoreAxisCorrelationAnalysis overallCorrelation =
                    new ScoreAxisCorrelationAnalysis();
            List<SeedAnalysis> seedAnalyses = analyzeSeeds(
                    scenario,
                    simulationCount,
                    randomSeeds,
                    overallCorrelation
            );
            writeSeedDetail(outputDirectory, seedAnalyses);
            writePersonaStability(outputDirectory, seedAnalyses);
            writeAxisCorrelation(outputDirectory, seedAnalyses, overallCorrelation);
        } catch (IOException exception) {
            throw new IllegalStateException("Seed 안정성 분석 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private List<SeedAnalysis> analyzeSeeds(
            ScenarioDto scenario,
            int simulationCount,
            List<Long> randomSeeds,
            ScoreAxisCorrelationAnalysis overallCorrelation) {
        List<SeedAnalysis> analyses = new ArrayList<>();
        for (Long randomSeed : randomSeeds) {
            ScoreAxisCorrelationAnalysis seedCorrelation =
                    new ScoreAxisCorrelationAnalysis();
            GameBehaviorSimulationAnalysis analysis =
                    simulationAnalyzer.analyzeGameSimulations(
                            scenario,
                            simulationCount,
                            randomSeed,
                            GameBehaviorFrequencyCondition.MEDIUM,
                            ConsecutiveActionMultiplierCondition.ENABLED,
                            TARGET_CONDITION.getSameTickRuleCondition(),
                            TARGET_CONDITION.getRuleAccumulationCondition(),
                            LossAveragingRtWeightCondition.RT_15,
                            TARGET_CONDITION.getRuleEvaluationCondition(),
                            TradeQuantityGenerationCondition.SYMMETRIC_THREE_LEVEL,
                            simulationResult -> {
                                seedCorrelation.addResult(simulationResult);
                                overallCorrelation.addResult(simulationResult);
                            }
                    );
            analyses.add(new SeedAnalysis(randomSeed, analysis, seedCorrelation));
        }
        return List.copyOf(analyses);
    }

    private void writeSeedDetail(
            Path outputDirectory,
            List<SeedAnalysis> seedAnalyses) throws IOException {
        try (BufferedWriter writer = createWriter(
                outputDirectory.resolve(SEED_DETAIL_FILE_NAME))) {
            List<String> header = new ArrayList<>(List.of(
                    "보정_조건(mitigation_condition)",
                    "Seed(random_seed)",
                    "사용자수(simulation_count)",
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
            writeCsvRow(writer, header);

            for (SeedAnalysis seedAnalysis : seedAnalyses) {
                GameBehaviorSimulationAnalysis analysis = seedAnalysis.analysis();
                List<String> row = new ArrayList<>(List.of(
                        TARGET_CONDITION.name(),
                        String.valueOf(seedAnalysis.randomSeed()),
                        String.valueOf(analysis.getTotalSimulationCount()),
                        analysis.getOverallRtScoreSummary().getAverage().toPlainString(),
                        analysis.getOverallLhScoreSummary().getAverage().toPlainString(),
                        analysis.getOverallRpScoreSummary().getAverage().toPlainString(),
                        seedAnalysis.correlation().getRtRpCorrelation().toPlainString(),
                        seedAnalysis.correlation().getRtLhCorrelation().toPlainString(),
                        seedAnalysis.correlation().getLhRpCorrelation().toPlainString()
                ));
                for (PersonaType personaType : PersonaType.values()) {
                    row.add(analysis.getPersonaSummary(personaType)
                            .getDistributionRate()
                            .toPlainString());
                }
                writeCsvRow(writer, row);
            }
        }
    }

    private void writePersonaStability(
            Path outputDirectory,
            List<SeedAnalysis> seedAnalyses) throws IOException {
        try (BufferedWriter writer = createWriter(
                outputDirectory.resolve(PERSONA_STABILITY_FILE_NAME))) {
            writeCsvRow(writer, List.of(
                    "보정_조건(mitigation_condition)",
                    "성향_코드(persona_code)",
                    "Seed수(seed_count)",
                    "평균_판정_비율(average_rate)",
                    "판정_비율_표준편차(standard_deviation)",
                    "최소_판정_비율(minimum_rate)",
                    "최대_판정_비율(maximum_rate)",
                    "검증_하한(target_minimum)",
                    "검증_상한(target_maximum)",
                    "모든_Seed_권장범위_충족(all_seeds_in_target_range)"
            ));
            for (PersonaType personaType : PersonaType.values()) {
                List<BigDecimal> distributionRates = seedAnalyses.stream()
                        .map(seedAnalysis -> seedAnalysis.analysis()
                                .getPersonaSummary(personaType)
                                .getDistributionRate())
                        .toList();
                RateSummary rateSummary = summarizeRates(distributionRates);
                TargetRange targetRange = getTargetRange(personaType);
                writeCsvRow(writer, List.of(
                        TARGET_CONDITION.name(),
                        personaType.name(),
                        String.valueOf(seedAnalyses.size()),
                        rateSummary.average().toPlainString(),
                        rateSummary.standardDeviation().toPlainString(),
                        rateSummary.minimum().toPlainString(),
                        rateSummary.maximum().toPlainString(),
                        targetRange == null ? "" : targetRange.minimum().toPlainString(),
                        targetRange == null ? "" : targetRange.maximum().toPlainString(),
                        targetRange == null
                                ? "해당 없음"
                                : String.valueOf(distributionRates.stream()
                                        .allMatch(targetRange::contains))
                ));
            }
        }
    }

    private void writeAxisCorrelation(
            Path outputDirectory,
            List<SeedAnalysis> seedAnalyses,
            ScoreAxisCorrelationAnalysis overallCorrelation) throws IOException {
        try (BufferedWriter writer = createWriter(
                outputDirectory.resolve(AXIS_CORRELATION_FILE_NAME))) {
            writeCsvRow(writer, List.of(
                    "보정_조건(mitigation_condition)",
                    "분석_범위(scope)",
                    "표본수(sample_count)",
                    "축_조합(axis_pair)",
                    "상관계수(correlation)",
                    "상관_방향(direction)",
                    "상관_강도(strength)"
            ));
            for (SeedAnalysis seedAnalysis : seedAnalyses) {
                writeCorrelationRows(
                        writer,
                        String.valueOf(seedAnalysis.randomSeed()),
                        seedAnalysis.correlation()
                );
            }
            writeCorrelationRows(writer, "전체 Seed 통합", overallCorrelation);
        }
    }

    private void writeCorrelationRows(
            BufferedWriter writer,
            String scope,
            ScoreAxisCorrelationAnalysis correlationAnalysis) throws IOException {
        writeCorrelationRow(
                writer,
                scope,
                correlationAnalysis.getSampleCount(),
                "RT-RP",
                correlationAnalysis.getRtRpCorrelation()
        );
        writeCorrelationRow(
                writer,
                scope,
                correlationAnalysis.getSampleCount(),
                "RT-LH",
                correlationAnalysis.getRtLhCorrelation()
        );
        writeCorrelationRow(
                writer,
                scope,
                correlationAnalysis.getSampleCount(),
                "LH-RP",
                correlationAnalysis.getLhRpCorrelation()
        );
    }

    private void writeCorrelationRow(
            BufferedWriter writer,
            String scope,
            long sampleCount,
            String axisPair,
            BigDecimal correlation) throws IOException {
        writeCsvRow(writer, List.of(
                TARGET_CONDITION.name(),
                scope,
                String.valueOf(sampleCount),
                axisPair,
                correlation.toPlainString(),
                getCorrelationDirection(correlation),
                getCorrelationStrength(correlation)
        ));
    }

    private RateSummary summarizeRates(List<BigDecimal> rates) {
        BigDecimal count = BigDecimal.valueOf(rates.size());
        BigDecimal average = rates.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(count, STATISTICS_CONTEXT);
        BigDecimal variance = rates.stream()
                .map(rate -> rate.subtract(average))
                .map(difference -> difference.multiply(difference))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(count, STATISTICS_CONTEXT);
        return new RateSummary(
                average.setScale(STATISTICS_SCALE, RoundingMode.HALF_UP),
                variance.sqrt(STATISTICS_CONTEXT)
                        .setScale(STATISTICS_SCALE, RoundingMode.HALF_UP),
                rates.stream().min(BigDecimal::compareTo).orElseThrow(),
                rates.stream().max(BigDecimal::compareTo).orElseThrow()
        );
    }

    private TargetRange getTargetRange(PersonaType personaType) {
        if (personaType == PersonaType.HLH) {
            return new TargetRange(BigDecimal.valueOf(25), BigDecimal.valueOf(30));
        }
        if (personaType == PersonaType.HHL
                || personaType == PersonaType.HLL
                || personaType == PersonaType.LLH
                || personaType == PersonaType.LLL) {
            return new TargetRange(BigDecimal.valueOf(2), BigDecimal.valueOf(6));
        }
        return null;
    }

    private String getCorrelationDirection(BigDecimal correlation) {
        int comparison = correlation.compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            return "양의 상관";
        }
        if (comparison < 0) {
            return "음의 상관";
        }
        return "상관 없음";
    }

    private String getCorrelationStrength(BigDecimal correlation) {
        BigDecimal absoluteCorrelation = correlation.abs();
        if (absoluteCorrelation.compareTo(BigDecimal.valueOf(0.2)) < 0) {
            return "매우 약함";
        }
        if (absoluteCorrelation.compareTo(BigDecimal.valueOf(0.4)) < 0) {
            return "약함";
        }
        if (absoluteCorrelation.compareTo(BigDecimal.valueOf(0.6)) < 0) {
            return "보통";
        }
        if (absoluteCorrelation.compareTo(BigDecimal.valueOf(0.8)) < 0) {
            return "강함";
        }
        return "매우 강함";
    }

    private BufferedWriter createWriter(Path outputPath) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
        writer.write(UTF_8_BYTE_ORDER_MARK);
        return writer;
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

    private void validateInput(
            int simulationCount,
            List<Long> randomSeeds,
            Path outputDirectory) {
        if (simulationCount <= 0) {
            throw new IllegalArgumentException("Seed별 시뮬레이션 수는 0보다 커야 합니다.");
        }
        if (randomSeeds == null || randomSeeds.size() < 3 || randomSeeds.size() > 5) {
            throw new IllegalArgumentException("Seed는 3개 이상 5개 이하로 지정해야 합니다.");
        }
        if (randomSeeds.stream().anyMatch(seed -> seed == null)) {
            throw new IllegalArgumentException("Seed 값은 필수입니다.");
        }
        Set<Long> uniqueSeeds = new HashSet<>(randomSeeds);
        if (uniqueSeeds.size() != randomSeeds.size()) {
            throw new IllegalArgumentException("Seed 값은 중복될 수 없습니다.");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Seed 안정성 CSV 출력 경로는 필수입니다.");
        }
    }

    private record SeedAnalysis(
            long randomSeed,
            GameBehaviorSimulationAnalysis analysis,
            ScoreAxisCorrelationAnalysis correlation) {
    }

    private record RateSummary(
            BigDecimal average,
            BigDecimal standardDeviation,
            BigDecimal minimum,
            BigDecimal maximum) {
    }

    private record TargetRange(BigDecimal minimum, BigDecimal maximum) {

        private boolean contains(BigDecimal value) {
            return value.compareTo(minimum) >= 0 && value.compareTo(maximum) <= 0;
        }
    }
}
