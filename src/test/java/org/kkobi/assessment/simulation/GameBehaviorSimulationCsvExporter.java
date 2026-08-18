package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.dto.ScenarioDto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameBehaviorSimulationCsvExporter {

    public static final String SIMULATION_RESULT_FILE_NAME = "simulation-results.csv";
    public static final String PERSONA_SUMMARY_FILE_NAME = "persona-summary.csv";
    public static final String BIAS_CAUSE_SUMMARY_FILE_NAME = "bias-cause-summary.csv";
    public static final String RULE_STATISTICS_FILE_NAME = "rule-statistics.csv";

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public GameBehaviorSimulationAnalysis exportGameSimulations(
            ScenarioDto scenario,
            int simulationCount,
            long randomSeed,
            Path outputDirectory) {
        return exportGameSimulations(
                scenario,
                simulationCount,
                randomSeed,
                outputDirectory,
                null
        );
    }

    public GameBehaviorSimulationAnalysis exportGameSimulations(
            ScenarioDto scenario,
            int simulationCount,
            long randomSeed,
            Path outputDirectory,
            GameBehaviorFrequencyCondition frequencyCondition) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("CSV 출력 경로는 필수입니다.");
        }

        try {
            Files.createDirectories(outputDirectory);
            Path simulationResultPath = outputDirectory.resolve(SIMULATION_RESULT_FILE_NAME);
            GameBehaviorSimulationAnalysis analysis;
            try (BufferedWriter simulationResultWriter = Files.newBufferedWriter(
                    simulationResultPath,
                    StandardCharsets.UTF_8
            )) {
                simulationResultWriter.write(UTF_8_BYTE_ORDER_MARK);
                writeSimulationResultHeader(simulationResultWriter);
                analysis = simulationAnalyzer.analyzeGameSimulations(
                        scenario,
                        simulationCount,
                        randomSeed,
                        frequencyCondition,
                        simulationResult -> writeSimulationResult(
                                simulationResultWriter,
                                simulationResult
                        )
                );
            }
            writePersonaSummary(
                    outputDirectory.resolve(PERSONA_SUMMARY_FILE_NAME),
                    analysis
            );
            writeBiasCauseSummary(
                    outputDirectory.resolve(BIAS_CAUSE_SUMMARY_FILE_NAME),
                    analysis
            );
            writeRuleStatistics(
                    outputDirectory.resolve(RULE_STATISTICS_FILE_NAME),
                    analysis
            );
            return analysis;
        } catch (UncheckedIOException exception) {
            throw new IllegalStateException(
                    "시뮬레이션 CSV를 저장하지 못했습니다.",
                    exception.getCause()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("시뮬레이션 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private void writeSimulationResultHeader(BufferedWriter writer) throws IOException {
        writeCsvRow(writer, List.of(
                "가상_사용자_ID(simulation_user_id)",
                "초기_현금_비율(initial_cash_ratio)",
                "초기_주식_비율(initial_stock_ratio)",
                "초기_예금_비율(initial_deposit_ratio)",
                "매수_횟수(buy_count)",
                "매도_횟수(sell_count)",
                "무행동_Tick_수(no_action_tick_count)",
                "총_매수_금액(total_buy_amount)",
                "총_매도_금액(total_sell_amount)",
                "전량_매도_횟수(full_sell_count)",
                "급락장_매수_횟수(crash_buy_count)",
                "급락장_전량_매도_횟수(crash_full_sell_count)",
                "급등장_매수_횟수(bull_buy_count)",
                "급등장_익절_횟수(bull_profit_sell_count)",
                "물타기_매수_횟수(loss_averaging_buy_count)",
                "손절_매도_횟수(loss_cut_sell_count)",
                "예금_중도해지_여부(deposit_cancelled)",
                "예금_만기유지_여부(deposit_matured)",
                "예금해지후_주식매수_여부(bought_stock_after_deposit_cancel)",
                "최대_연속_매수_횟수(maximum_consecutive_buy_count)",
                "최대_연속_매도_횟수(maximum_consecutive_sell_count)",
                "연속행동_2회_발생수(consecutive_action_level_two_count)",
                "연속행동_3회이상_발생수(consecutive_action_level_three_or_more_count)",
                "최종_RT_점수(final_rt_score)",
                "최종_LH_점수(final_lh_score)",
                "최종_RP_점수(final_rp_score)",
                "최종_성향_코드(persona_type)",
                "적용_규칙별_횟수(rule_application_counts)"
        ));
    }

    private void writeSimulationResult(
            BufferedWriter writer,
            GameBehaviorSimulationResult result) {
        try {
            writeCsvRow(writer, List.of(
                    String.valueOf(result.getSimulationUserId()),
                    result.getInitialCashRatio().toPlainString(),
                    result.getInitialStockRatio().toPlainString(),
                    result.getInitialDepositRatio().toPlainString(),
                    String.valueOf(result.getBuyCount()),
                    String.valueOf(result.getSellCount()),
                    String.valueOf(result.getNoActionTickCount()),
                    String.valueOf(result.getTotalBuyAmount()),
                    String.valueOf(result.getTotalSellAmount()),
                    String.valueOf(result.getFullSellCount()),
                    String.valueOf(result.getCrashBuyCount()),
                    String.valueOf(result.getCrashFullSellCount()),
                    String.valueOf(result.getBullBuyCount()),
                    String.valueOf(result.getBullProfitSellCount()),
                    String.valueOf(result.getLossAveragingBuyCount()),
                    String.valueOf(result.getLossCutSellCount()),
                    String.valueOf(result.isDepositCancelled()),
                    String.valueOf(result.isDepositMatured()),
                    String.valueOf(result.isBoughtStockAfterDepositCancel()),
                    String.valueOf(result.getMaximumConsecutiveBuyCount()),
                    String.valueOf(result.getMaximumConsecutiveSellCount()),
                    String.valueOf(result.getConsecutiveActionLevelTwoCount()),
                    String.valueOf(result.getConsecutiveActionLevelThreeOrMoreCount()),
                    result.getFinalRtScore().toPlainString(),
                    result.getFinalLhScore().toPlainString(),
                    result.getFinalRpScore().toPlainString(),
                    result.getPersonaType().name(),
                    createRuleApplicationCounts(result.getRuleApplicationCounts())
            ));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private String createRuleApplicationCounts(
            Map<BehaviorRuleCode, Integer> ruleApplicationCounts) {
        return Arrays.stream(BehaviorRuleCode.values())
                .filter(ruleApplicationCounts::containsKey)
                .map(ruleCode -> ruleCode.name() + "=" + ruleApplicationCounts.get(ruleCode))
                .collect(Collectors.joining("|"));
    }

    private void writePersonaSummary(
            Path personaSummaryPath,
            GameBehaviorSimulationAnalysis analysis) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                personaSummaryPath,
                StandardCharsets.UTF_8
        )) {
            writer.write(UTF_8_BYTE_ORDER_MARK);
            writeCsvRow(writer, createPersonaSummaryHeader());
            writeCsvRow(writer, createOverallSummaryRow(analysis));
            for (PersonaType personaType : PersonaType.values()) {
                writeCsvRow(
                        writer,
                        createPersonaSummaryRow(analysis.getPersonaSummary(personaType))
                );
            }
        }
    }

    private List<String> createPersonaSummaryHeader() {
        return List.of(
                "성향_코드(persona_type)",
                "사용자_수(simulation_count)",
                "전체_분포_비율(distribution_rate)",
                "RT_평균(rt_average)",
                "RT_표준편차(rt_standard_deviation)",
                "RT_최솟값(rt_minimum)",
                "RT_최댓값(rt_maximum)",
                "LH_평균(lh_average)",
                "LH_표준편차(lh_standard_deviation)",
                "LH_최솟값(lh_minimum)",
                "LH_최댓값(lh_maximum)",
                "RP_평균(rp_average)",
                "RP_표준편차(rp_standard_deviation)",
                "RP_최솟값(rp_minimum)",
                "RP_최댓값(rp_maximum)"
        );
    }

    private List<String> createOverallSummaryRow(GameBehaviorSimulationAnalysis analysis) {
        return createSummaryRow(
                "ALL",
                analysis.getTotalSimulationCount(),
                BigDecimal.valueOf(100),
                analysis.getOverallRtScoreSummary(),
                analysis.getOverallLhScoreSummary(),
                analysis.getOverallRpScoreSummary()
        );
    }

    private List<String> createPersonaSummaryRow(
            GameBehaviorSimulationAnalysis.PersonaSummary personaSummary) {
        return createSummaryRow(
                personaSummary.getPersonaType().name(),
                personaSummary.getSimulationCount(),
                personaSummary.getDistributionRate(),
                personaSummary.getRtScoreSummary(),
                personaSummary.getLhScoreSummary(),
                personaSummary.getRpScoreSummary()
        );
    }

    private List<String> createSummaryRow(
            String personaType,
            int simulationCount,
            BigDecimal distributionRate,
            GameBehaviorSimulationAnalysis.ScoreSummary rtScoreSummary,
            GameBehaviorSimulationAnalysis.ScoreSummary lhScoreSummary,
            GameBehaviorSimulationAnalysis.ScoreSummary rpScoreSummary) {
        List<String> rowValues = new ArrayList<>();
        rowValues.add(personaType);
        rowValues.add(String.valueOf(simulationCount));
        rowValues.add(distributionRate.toPlainString());
        addScoreSummary(rowValues, rtScoreSummary);
        addScoreSummary(rowValues, lhScoreSummary);
        addScoreSummary(rowValues, rpScoreSummary);
        return rowValues;
    }

    private void addScoreSummary(
            List<String> rowValues,
            GameBehaviorSimulationAnalysis.ScoreSummary scoreSummary) {
        rowValues.add(scoreSummary.getAverage().toPlainString());
        rowValues.add(scoreSummary.getStandardDeviation().toPlainString());
        rowValues.add(scoreSummary.getMinimum().toPlainString());
        rowValues.add(scoreSummary.getMaximum().toPlainString());
    }

    private void writeBiasCauseSummary(
            Path biasCauseSummaryPath,
            GameBehaviorSimulationAnalysis analysis) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                biasCauseSummaryPath,
                StandardCharsets.UTF_8
        )) {
            writer.write(UTF_8_BYTE_ORDER_MARK);
            writeCsvRow(writer, List.of(
                    "지표_코드(metric_code)",
                    "지표_설명(metric_name)",
                    "해당_사용자수_또는_발생수(count)",
                    "비율_또는_평균값(rate_or_average)",
                    "집계_기준(criteria)"
            ));

            GameBehaviorSimulationAnalysis.BehaviorStatistics behaviorStatistics =
                    analysis.getBehaviorStatistics();
            writeMetricRow(writer, "average_buy_count", "사용자당 평균 매수 횟수", "",
                    behaviorStatistics.getAverageBuyCount(), "사용자 1명 기준 평균");
            writeMetricRow(writer, "average_sell_count", "사용자당 평균 매도 횟수", "",
                    behaviorStatistics.getAverageSellCount(), "사용자 1명 기준 평균");
            writeMetricRow(writer, "average_no_action_tick_count",
                    "사용자당 평균 무행동 Tick 수", "",
                    behaviorStatistics.getAverageNoActionTickCount(), "사용자 1명 기준 평균");
            writeMetricRow(writer, "average_action_count_per_tick", "Tick당 평균 행동 횟수", "",
                    behaviorStatistics.getAverageActionCountPerTick(),
                    "매수·매도·예금 해지 횟수 / 전체 52 Tick");
            writeMetricRow(writer, "consecutive_action_level_two_count",
                    "연속 행동 2회 발생 횟수",
                    String.valueOf(behaviorStatistics.getConsecutiveActionLevelTwoCount()),
                    null, "같은 시장 상태에서 같은 행동이 2회 연속 발생");
            writeMetricRow(writer, "consecutive_action_level_three_or_more_count",
                    "연속 행동 3회 이상 발생 횟수",
                    String.valueOf(behaviorStatistics.getConsecutiveActionLevelThreeOrMoreCount()),
                    null, "같은 시장 상태에서 같은 행동이 3회 이상 연속 발생");

            writeScoreDiagnostic(writer, "rt", analysis.getRtScoreDiagnostic());
            writeScoreDiagnostic(writer, "lh", analysis.getLhScoreDiagnostic());
            writeScoreDiagnostic(writer, "rp", analysis.getRpScoreDiagnostic());
        }
    }

    private void writeScoreDiagnostic(
            BufferedWriter writer,
            String scoreAxis,
            GameBehaviorSimulationAnalysis.ScoreDiagnostic scoreDiagnostic) throws IOException {
        writeMetricRow(
                writer,
                scoreAxis + "_maximum_score",
                scoreAxis.toUpperCase() + " 100점 도달",
                String.valueOf(scoreDiagnostic.getMaximumScoreCount()),
                scoreDiagnostic.getMaximumScoreRate(),
                "최종 점수 = 100점"
        );
        writeMetricRow(
                writer,
                scoreAxis + "_near_boundary",
                scoreAxis.toUpperCase() + " High/Low 경계 근처",
                String.valueOf(scoreDiagnostic.getBoundaryScoreCount()),
                scoreDiagnostic.getBoundaryScoreRate(),
                "최종 점수 45점 이상 55점 이하"
        );
    }

    private void writeMetricRow(
            BufferedWriter writer,
            String metric,
            String metricName,
            String count,
            BigDecimal rateOrAverage,
            String criteria) throws IOException {
        writeCsvRow(writer, List.of(
                metric,
                metricName,
                count,
                rateOrAverage == null ? "" : rateOrAverage.toPlainString(),
                criteria
        ));
    }

    private void writeRuleStatistics(
            Path ruleStatisticsPath,
            GameBehaviorSimulationAnalysis analysis) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                ruleStatisticsPath,
                StandardCharsets.UTF_8
        )) {
            writer.write(UTF_8_BYTE_ORDER_MARK);
            writeCsvRow(writer, List.of(
                    "규칙_코드(rule_code)",
                    "규칙_설명(rule_name)",
                    "전체_적용_횟수(application_count)",
                    "RT_총기여도(rt_total_contribution)",
                    "LH_총기여도(lh_total_contribution)",
                    "RP_총기여도(rp_total_contribution)"
            ));
            for (BehaviorRuleCode ruleCode : BehaviorRuleCode.values()) {
                GameBehaviorSimulationAnalysis.RuleStatistics ruleStatistics =
                        analysis.getRuleStatistics().get(ruleCode);
                writeCsvRow(writer, List.of(
                        ruleCode.name(),
                        getRuleName(ruleCode),
                        String.valueOf(ruleStatistics.getApplicationCount()),
                        ruleStatistics.getRtTotalContribution().toPlainString(),
                        ruleStatistics.getLhTotalContribution().toPlainString(),
                        ruleStatistics.getRpTotalContribution().toPlainString()
                ));
            }
        }
    }

    static String getRuleName(BehaviorRuleCode ruleCode) {
        return switch (ruleCode) {
            case INITIAL_STOCK_ALLOCATION -> "초기 주식 비중 70% 이상";
            case INITIAL_DEPOSIT_ALLOCATION -> "초기 예금 비중 50% 이상";
            case INITIAL_CASH_ALLOCATION -> "초기 현금 비중 30% 이상";
            case SEVEN_DAY_STOCK_ALLOCATION -> "첫 거래 후 7일 평균 주식 비중 70% 이상";
            case SEVEN_DAY_CASH_ALLOCATION -> "첫 거래 후 7일 평균 현금 비중 30% 이상";
            case CRASH_BUY -> "급락장에서 주식 추가 매수";
            case CRASH_FULL_SELL -> "급락장에서 주식 전량 매도";
            case BULL_BUY -> "급등장에서 주식 추격 매수";
            case BULL_PROFIT_SELL -> "급등장에서 수익 실현 매도";
            case VOLATILE_DAY_TRADE -> "변동성장에서 당일 매매";
            case DEPOSIT_CANCEL_AND_SECURITY_BUY -> "예금 해지 후 주식 매수";
            case DEPOSIT_MATURITY -> "예금 만기까지 유지";
            case SHORT_SECURITY_HOLDING -> "주식 단기 보유";
            case LONG_SECURITY_HOLDING -> "주식 장기 보유";
            case LOSS_AVERAGING_BUY -> "손실 종목 추가 매수(물타기)";
            case LOSS_CUT_SELL -> "손실 종목 손절 매도";
            case STOCK_ROTATION -> "매도 후 다른 종목으로 교체";
            case VERY_LOW_CASH_MAINTENANCE -> "현금 비중 5% 미만 유지";
            case MEDIUM_CASH_MAINTENANCE -> "현금 비중 25~49% 유지";
            case HIGH_CASH_MAINTENANCE -> "현금 비중 50% 이상 유지";
            case HIGH_TRADE_FREQUENCY -> "일평균 거래 횟수 5회 이상";
            case LOW_TRADE_FREQUENCY -> "일평균 거래 횟수 0.2회 이하";
            case CRASH_HOLDING -> "급락장 보유 유지";
            case NORMAL_PLANNED_BUY -> "정상장 계획 매수";
            case CASH_BUFFER_MAINTENANCE -> "현금 완충 유지";
            case RISK_BUDGET_MAINTENANCE -> "위험 예산 유지";
            case HHL_COMPOSITE -> "HHL 복합 비추격";
            case HLL_NO_CHASE -> "HLL 고위험 노출 비추격";
            case LHH_COMPLETED_OPPORTUNITY -> "LHH 완결형 기회 실행";
            case NORMAL_PARTIAL_SELL -> "정상장 부분 매도·유동성 확보";
            case DEPOSIT_CANCEL_CASH_RETENTION -> "예금 해지 후 현금 유지";
        };
    }

    private void writeCsvRow(
            BufferedWriter writer,
            List<String> values) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException("CSV 작성기는 필수입니다.");
        }
        if (values == null) {
            throw new IllegalArgumentException("CSV 행 값은 필수입니다.");
        }
        writer.write(values.stream()
                .map(this::escapeCsvValue)
                .collect(Collectors.joining(",")));
        writer.newLine();
    }

    private String escapeCsvValue(String value) {
        String safeValue = value == null ? "" : value;
        if (!safeValue.contains(",")
                && !safeValue.contains("\"")
                && !safeValue.contains("\n")
                && !safeValue.contains("\r")) {
            return safeValue;
        }
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
