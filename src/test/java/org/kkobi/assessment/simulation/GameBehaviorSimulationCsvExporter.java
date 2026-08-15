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

    private static final char UTF_8_BYTE_ORDER_MARK = '\uFEFF';

    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    public GameBehaviorSimulationAnalysis exportGameSimulations(
            ScenarioDto scenario,
            int simulationCount,
            long randomSeed,
            Path outputDirectory) {
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
                "simulation_user_id",
                "initial_cash_ratio",
                "initial_stock_ratio",
                "initial_deposit_ratio",
                "buy_count",
                "sell_count",
                "no_action_tick_count",
                "total_buy_amount",
                "total_sell_amount",
                "full_sell_count",
                "crash_buy_count",
                "crash_full_sell_count",
                "bull_buy_count",
                "bull_profit_sell_count",
                "loss_averaging_buy_count",
                "loss_cut_sell_count",
                "deposit_cancelled",
                "deposit_matured",
                "bought_stock_after_deposit_cancel",
                "maximum_consecutive_buy_count",
                "maximum_consecutive_sell_count",
                "final_rt_score",
                "final_lh_score",
                "final_rp_score",
                "persona_type",
                "rule_application_counts"
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
                "persona_type",
                "simulation_count",
                "distribution_rate",
                "rt_average",
                "rt_standard_deviation",
                "rt_minimum",
                "rt_maximum",
                "lh_average",
                "lh_standard_deviation",
                "lh_minimum",
                "lh_maximum",
                "rp_average",
                "rp_standard_deviation",
                "rp_minimum",
                "rp_maximum"
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
