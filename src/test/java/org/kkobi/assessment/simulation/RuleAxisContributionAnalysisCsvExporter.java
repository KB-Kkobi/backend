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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RuleAxisContributionAnalysisCsvExporter {

    public static final String RULE_DETAIL_FILE_NAME = "rule-axis-contribution-detail.csv";
    public static final String GROUP_SUMMARY_FILE_NAME = "rule-axis-contribution-summary.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';
    private static final int CALCULATION_SCALE = 4;

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public void exportRuleAxisContributionAnalysis(
            ScenarioDto scenario,
            int simulationCountPerCondition,
            long randomSeed,
            Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("규칙 축 기여도 CSV 출력 경로는 필수입니다.");
        }

        try {
            Files.createDirectories(outputDirectory);
            try (BufferedWriter detailWriter = createWriter(
                    outputDirectory.resolve(RULE_DETAIL_FILE_NAME)
            ); BufferedWriter summaryWriter = createWriter(
                    outputDirectory.resolve(GROUP_SUMMARY_FILE_NAME)
            )) {
                writeCsvRow(detailWriter, createDetailHeader());
                writeCsvRow(summaryWriter, createSummaryHeader());
                for (GameBehaviorFrequencyCondition frequencyCondition
                        : GameBehaviorFrequencyCondition.values()) {
                    GameBehaviorSimulationAnalysis analysis =
                            simulationAnalyzer.analyzeGameSimulations(
                                    scenario,
                                    simulationCountPerCondition,
                                    randomSeed,
                                    frequencyCondition
                            );
                    writeRuleDetails(detailWriter, frequencyCondition, analysis);
                    writeGroupSummaries(summaryWriter, frequencyCondition, analysis);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("규칙 축 기여도 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private BufferedWriter createWriter(Path path) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        writer.write(UTF_8_BYTE_ORDER_MARK);
        return writer;
    }

    private void writeRuleDetails(
            BufferedWriter writer,
            GameBehaviorFrequencyCondition frequencyCondition,
            GameBehaviorSimulationAnalysis analysis) throws IOException {
        for (BehaviorRuleCode ruleCode : BehaviorRuleCode.values()) {
            GameBehaviorSimulationAnalysis.RuleStatistics ruleStatistics =
                    analysis.getRuleStatistics().get(ruleCode);
            writeCsvRow(writer, createDetailRow(
                    frequencyCondition,
                    analysis.getTotalSimulationCount(),
                    ruleStatistics
            ));
        }
    }

    private List<String> createDetailHeader() {
        return List.of(
                "빈도_조건_코드(frequency_condition)",
                "빈도_조건_설명(frequency_name)",
                "규칙_코드(rule_code)",
                "규칙_설명(rule_name)",
                "행동_그룹(action_group)",
                "시장상황_그룹(market_context_group)",
                "전체_적용수(application_count)",
                "사용자당_평균_적용수(application_count_per_user)",
                "RT_총기여도(rt_total_contribution)",
                "RT_1회당_기여도(rt_contribution_per_application)",
                "LH_총기여도(lh_total_contribution)",
                "LH_1회당_기여도(lh_contribution_per_application)",
                "RP_총기여도(rp_total_contribution)",
                "RP_1회당_기여도(rp_contribution_per_application)"
        );
    }

    private List<String> createDetailRow(
            GameBehaviorFrequencyCondition frequencyCondition,
            int simulationCount,
            GameBehaviorSimulationAnalysis.RuleStatistics statistics) {
        long applicationCount = statistics.getApplicationCount();
        return List.of(
                frequencyCondition.name(),
                frequencyCondition.getDescription(),
                statistics.getRuleCode().name(),
                GameBehaviorSimulationCsvExporter.getRuleName(statistics.getRuleCode()),
                getActionGroup(statistics.getRuleCode()).groupName,
                getMarketContextGroup(statistics.getRuleCode()).groupName,
                String.valueOf(applicationCount),
                divide(BigDecimal.valueOf(applicationCount), simulationCount).toPlainString(),
                statistics.getRtTotalContribution().toPlainString(),
                divide(statistics.getRtTotalContribution(), applicationCount).toPlainString(),
                statistics.getLhTotalContribution().toPlainString(),
                divide(statistics.getLhTotalContribution(), applicationCount).toPlainString(),
                statistics.getRpTotalContribution().toPlainString(),
                divide(statistics.getRpTotalContribution(), applicationCount).toPlainString()
        );
    }

    private void writeGroupSummaries(
            BufferedWriter writer,
            GameBehaviorFrequencyCondition frequencyCondition,
            GameBehaviorSimulationAnalysis analysis) throws IOException {
        EnumMap<ContributionGroup, ContributionAccumulator> accumulators =
                createContributionAccumulators();
        for (GameBehaviorSimulationAnalysis.RuleStatistics ruleStatistics
                : analysis.getRuleStatistics().values()) {
            accumulators.get(getActionGroup(ruleStatistics.getRuleCode()))
                    .addRuleStatistics(ruleStatistics);
            accumulators.get(getMarketContextGroup(ruleStatistics.getRuleCode()))
                    .addRuleStatistics(ruleStatistics);
        }
        for (ContributionGroup group : ContributionGroup.values()) {
            ContributionAccumulator accumulator = accumulators.get(group);
            if (accumulator.applicationCount == 0) {
                continue;
            }
            writeCsvRow(
                    writer,
                    createSummaryRow(
                            frequencyCondition,
                            analysis.getTotalSimulationCount(),
                            group,
                            accumulator
                    )
            );
        }
    }

    private EnumMap<ContributionGroup, ContributionAccumulator>
            createContributionAccumulators() {
        EnumMap<ContributionGroup, ContributionAccumulator> accumulators =
                new EnumMap<>(ContributionGroup.class);
        for (ContributionGroup group : ContributionGroup.values()) {
            accumulators.put(group, new ContributionAccumulator());
        }
        return accumulators;
    }

    private List<String> createSummaryHeader() {
        return List.of(
                "빈도_조건_코드(frequency_condition)",
                "빈도_조건_설명(frequency_name)",
                "분류_기준(group_dimension)",
                "그룹_코드(group_code)",
                "그룹_설명(group_name)",
                "전체_규칙_적용수(application_count)",
                "사용자당_평균_규칙수(application_count_per_user)",
                "RT_양수_기여(rt_positive_contribution)",
                "RT_음수_기여_절댓값(rt_negative_contribution_absolute)",
                "RT_순기여(rt_net_contribution)",
                "RT_사용자당_순기여(rt_net_per_user)",
                "RT_1회당_순기여(rt_net_per_application)",
                "LH_양수_기여(lh_positive_contribution)",
                "LH_음수_기여_절댓값(lh_negative_contribution_absolute)",
                "LH_순기여(lh_net_contribution)",
                "LH_사용자당_순기여(lh_net_per_user)",
                "LH_1회당_순기여(lh_net_per_application)",
                "RP_양수_기여(rp_positive_contribution)",
                "RP_음수_기여_절댓값(rp_negative_contribution_absolute)",
                "RP_순기여(rp_net_contribution)",
                "RP_사용자당_순기여(rp_net_per_user)",
                "RP_1회당_순기여(rp_net_per_application)"
        );
    }

    private List<String> createSummaryRow(
            GameBehaviorFrequencyCondition frequencyCondition,
            int simulationCount,
            ContributionGroup group,
            ContributionAccumulator accumulator) {
        List<String> row = new ArrayList<>(List.of(
                frequencyCondition.name(),
                frequencyCondition.getDescription(),
                group.dimensionName,
                group.name(),
                group.groupName,
                String.valueOf(accumulator.applicationCount),
                divide(BigDecimal.valueOf(accumulator.applicationCount), simulationCount)
                        .toPlainString()
        ));
        addAxisSummary(row, accumulator.rtContribution, simulationCount,
                accumulator.applicationCount);
        addAxisSummary(row, accumulator.lhContribution, simulationCount,
                accumulator.applicationCount);
        addAxisSummary(row, accumulator.rpContribution, simulationCount,
                accumulator.applicationCount);
        return row;
    }

    private void addAxisSummary(
            List<String> row,
            AxisContribution contribution,
            int simulationCount,
            long applicationCount) {
        BigDecimal netContribution = contribution.positiveContribution
                .subtract(contribution.negativeContributionAbsolute);
        row.add(contribution.positiveContribution.toPlainString());
        row.add(contribution.negativeContributionAbsolute.toPlainString());
        row.add(netContribution.toPlainString());
        row.add(divide(netContribution, simulationCount).toPlainString());
        row.add(divide(netContribution, applicationCount).toPlainString());
    }

    private BigDecimal divide(BigDecimal value, long divisor) {
        if (divisor == 0) {
            return BigDecimal.ZERO.setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
        }
        return value.divide(
                BigDecimal.valueOf(divisor),
                CALCULATION_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private ContributionGroup getActionGroup(BehaviorRuleCode ruleCode) {
        return switch (ruleCode) {
            case CRASH_BUY,
                    BULL_BUY,
                    LOSS_AVERAGING_BUY,
                    DEPOSIT_CANCEL_AND_SECURITY_BUY -> ContributionGroup.ACTION_BUY;
            case CRASH_FULL_SELL,
                    BULL_PROFIT_SELL,
                    LOSS_CUT_SELL -> ContributionGroup.ACTION_SELL;
            case INITIAL_STOCK_ALLOCATION,
                    INITIAL_DEPOSIT_ALLOCATION,
                    INITIAL_CASH_ALLOCATION -> ContributionGroup.ACTION_INITIAL_ALLOCATION;
            case DEPOSIT_MATURITY -> ContributionGroup.ACTION_DEPOSIT;
            default -> ContributionGroup.ACTION_OTHER;
        };
    }

    private ContributionGroup getMarketContextGroup(BehaviorRuleCode ruleCode) {
        return switch (ruleCode) {
            case CRASH_BUY, CRASH_FULL_SELL -> ContributionGroup.CONTEXT_CRASH;
            case BULL_BUY, BULL_PROFIT_SELL -> ContributionGroup.CONTEXT_BULL;
            case LOSS_AVERAGING_BUY, LOSS_CUT_SELL -> ContributionGroup.CONTEXT_LOSS_RESPONSE;
            case INITIAL_STOCK_ALLOCATION,
                    INITIAL_DEPOSIT_ALLOCATION,
                    INITIAL_CASH_ALLOCATION -> ContributionGroup.CONTEXT_INITIAL_ALLOCATION;
            case DEPOSIT_CANCEL_AND_SECURITY_BUY,
                    DEPOSIT_MATURITY -> ContributionGroup.CONTEXT_DEPOSIT;
            default -> ContributionGroup.CONTEXT_OTHER;
        };
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

    private enum ContributionGroup {
        ACTION_BUY("행동 종류", "매수 행동"),
        ACTION_SELL("행동 종류", "매도 행동"),
        ACTION_INITIAL_ALLOCATION("행동 종류", "초기 자산 배분"),
        ACTION_DEPOSIT("행동 종류", "예금 행동"),
        ACTION_OTHER("행동 종류", "기타 행동"),
        CONTEXT_CRASH("시장·상황", "급락장"),
        CONTEXT_BULL("시장·상황", "급등장"),
        CONTEXT_LOSS_RESPONSE("시장·상황", "손익 대응"),
        CONTEXT_INITIAL_ALLOCATION("시장·상황", "초기 자산 배분"),
        CONTEXT_DEPOSIT("시장·상황", "예금 관련"),
        CONTEXT_OTHER("시장·상황", "기타 상황");

        private final String dimensionName;
        private final String groupName;

        ContributionGroup(String dimensionName, String groupName) {
            this.dimensionName = dimensionName;
            this.groupName = groupName;
        }
    }

    private static class ContributionAccumulator {

        private long applicationCount;
        private final AxisContribution rtContribution = new AxisContribution();
        private final AxisContribution lhContribution = new AxisContribution();
        private final AxisContribution rpContribution = new AxisContribution();

        private void addRuleStatistics(
                GameBehaviorSimulationAnalysis.RuleStatistics ruleStatistics) {
            applicationCount += ruleStatistics.getApplicationCount();
            rtContribution.addContribution(ruleStatistics.getRtTotalContribution());
            lhContribution.addContribution(ruleStatistics.getLhTotalContribution());
            rpContribution.addContribution(ruleStatistics.getRpTotalContribution());
        }
    }

    private static class AxisContribution {

        private BigDecimal positiveContribution = BigDecimal.ZERO;
        private BigDecimal negativeContributionAbsolute = BigDecimal.ZERO;

        private void addContribution(BigDecimal contribution) {
            if (contribution.signum() >= 0) {
                positiveContribution = positiveContribution.add(contribution);
            } else {
                negativeContributionAbsolute = negativeContributionAbsolute
                        .add(contribution.abs());
            }
        }
    }
}
