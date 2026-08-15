package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.game.dto.ScenarioDto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RuleApplicationPercentileCsvExporter {

    public static final String RULE_PERCENTILE_FILE_NAME =
            "rule-application-percentiles.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';
    private static final int AVERAGE_SCALE = 4;
    private static final int RATE_SCALE = 2;

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public void exportRuleApplicationPercentiles(
            ScenarioDto scenario,
            int simulationCountPerCondition,
            long randomSeed,
            Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("규칙 적용 백분위 CSV 출력 경로는 필수입니다.");
        }

        try {
            Files.createDirectories(outputDirectory);
            try (BufferedWriter writer = Files.newBufferedWriter(
                    outputDirectory.resolve(RULE_PERCENTILE_FILE_NAME),
                    StandardCharsets.UTF_8
            )) {
                writer.write(UTF_8_BYTE_ORDER_MARK);
                writeCsvRow(writer, createHeader());
                for (GameBehaviorFrequencyCondition frequencyCondition
                        : GameBehaviorFrequencyCondition.values()) {
                    Map<BehaviorRuleCode, List<Integer>> applicationCounts =
                            collectRuleApplicationCounts(
                                    scenario,
                                    simulationCountPerCondition,
                                    randomSeed,
                                    frequencyCondition
                            );
                    for (BehaviorRuleCode ruleCode : BehaviorRuleCode.values()) {
                        writeCsvRow(
                                writer,
                                createRulePercentileRow(
                                        frequencyCondition,
                                        ruleCode,
                                        applicationCounts.get(ruleCode)
                                )
                        );
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("규칙 적용 백분위 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private Map<BehaviorRuleCode, List<Integer>> collectRuleApplicationCounts(
            ScenarioDto scenario,
            int simulationCount,
            long randomSeed,
            GameBehaviorFrequencyCondition frequencyCondition) {
        EnumMap<BehaviorRuleCode, List<Integer>> applicationCounts =
                createRuleApplicationCounts();
        simulationAnalyzer.analyzeGameSimulations(
                scenario,
                simulationCount,
                randomSeed,
                frequencyCondition,
                ConsecutiveActionMultiplierCondition.ENABLED,
                SameTickRuleApplicationCondition.REPEATED,
                simulationResult -> addRuleApplicationCounts(
                        applicationCounts,
                        simulationResult
                )
        );
        applicationCounts.values().forEach(Collections::sort);
        return applicationCounts;
    }

    private EnumMap<BehaviorRuleCode, List<Integer>> createRuleApplicationCounts() {
        EnumMap<BehaviorRuleCode, List<Integer>> applicationCounts =
                new EnumMap<>(BehaviorRuleCode.class);
        for (BehaviorRuleCode ruleCode : BehaviorRuleCode.values()) {
            applicationCounts.put(ruleCode, new ArrayList<>());
        }
        return applicationCounts;
    }

    private void addRuleApplicationCounts(
            Map<BehaviorRuleCode, List<Integer>> applicationCounts,
            GameBehaviorSimulationResult simulationResult) {
        for (BehaviorRuleCode ruleCode : BehaviorRuleCode.values()) {
            applicationCounts.get(ruleCode).add(
                    simulationResult.getRuleApplicationCounts().getOrDefault(ruleCode, 0)
            );
        }
    }

    private List<String> createHeader() {
        return List.of(
                "빈도_조건_코드(frequency_condition)",
                "빈도_조건_설명(frequency_name)",
                "규칙_코드(rule_code)",
                "규칙_설명(rule_name)",
                "전체_사용자수(total_user_count)",
                "규칙_적용_사용자수(applied_user_count)",
                "규칙_적용_사용자_비율(applied_user_rate)",
                "전체_사용자_평균_적용수(all_user_average)",
                "전체_사용자_중앙값(all_user_median)",
                "전체_사용자_P90(all_user_p90)",
                "전체_사용자_P95(all_user_p95)",
                "적용_사용자_평균_적용수(applied_user_average)",
                "적용_사용자_중앙값(applied_user_median)",
                "적용_사용자_P90(applied_user_p90)",
                "적용_사용자_P95(applied_user_p95)",
                "최대_적용수(maximum_application_count)",
                "상한_후보(applied_user_p95_cap_candidate)"
        );
    }

    private List<String> createRulePercentileRow(
            GameBehaviorFrequencyCondition frequencyCondition,
            BehaviorRuleCode ruleCode,
            List<Integer> allUserCounts) {
        List<Integer> appliedUserCounts = allUserCounts.stream()
                .filter(count -> count > 0)
                .toList();
        int maximumCount = allUserCounts.isEmpty()
                ? 0
                : allUserCounts.get(allUserCounts.size() - 1);
        int capCandidate = calculatePercentile(appliedUserCounts, 95);

        return List.of(
                frequencyCondition.name(),
                frequencyCondition.getDescription(),
                ruleCode.name(),
                GameBehaviorSimulationCsvExporter.getRuleName(ruleCode),
                String.valueOf(allUserCounts.size()),
                String.valueOf(appliedUserCounts.size()),
                calculateRate(appliedUserCounts.size(), allUserCounts.size()).toPlainString(),
                calculateAverage(allUserCounts).toPlainString(),
                calculateMedian(allUserCounts).toPlainString(),
                String.valueOf(calculatePercentile(allUserCounts, 90)),
                String.valueOf(calculatePercentile(allUserCounts, 95)),
                calculateAverage(appliedUserCounts).toPlainString(),
                calculateMedian(appliedUserCounts).toPlainString(),
                String.valueOf(calculatePercentile(appliedUserCounts, 90)),
                String.valueOf(capCandidate),
                String.valueOf(maximumCount),
                String.valueOf(capCandidate)
        );
    }

    private BigDecimal calculateAverage(List<Integer> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO.setScale(AVERAGE_SCALE, RoundingMode.HALF_UP);
        }
        long total = values.stream().mapToLong(Integer::longValue).sum();
        return BigDecimal.valueOf(total).divide(
                BigDecimal.valueOf(values.size()),
                AVERAGE_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal calculateMedian(List<Integer> sortedValues) {
        if (sortedValues.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int middleIndex = sortedValues.size() / 2;
        if (sortedValues.size() % 2 == 1) {
            return BigDecimal.valueOf(sortedValues.get(middleIndex))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(sortedValues.get(middleIndex - 1))
                .add(BigDecimal.valueOf(sortedValues.get(middleIndex)))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private int calculatePercentile(
            List<Integer> sortedValues,
            int percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        int rank = (int) Math.ceil(sortedValues.size() * percentile / 100.0);
        return sortedValues.get(Math.max(0, rank - 1));
    }

    private BigDecimal calculateRate(int count, int totalCount) {
        if (totalCount == 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(totalCount),
                        RATE_SCALE,
                        RoundingMode.HALF_UP
                );
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
