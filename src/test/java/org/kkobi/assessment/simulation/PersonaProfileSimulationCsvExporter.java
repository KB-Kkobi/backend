package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.PersonaType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PersonaProfileSimulationCsvExporter {

    private static final String BOM = "\uFEFF";

    public void export(
            Path outputDirectory,
            List<PersonaProfileSimulationResult> results) {
        try {
            Files.createDirectories(outputDirectory);
            writeResults(outputDirectory.resolve("persona-profile-results.csv"), results);
            writeConfusionMatrix(
                    outputDirectory.resolve("persona-confusion-matrix.csv"),
                    new PersonaProfileSimulationAnalysis(results)
            );
            PersonaStandardizationAnalysis standardization =
                    new PersonaStandardizationAnalysis(results);
            writeStandardizationSummary(
                    outputDirectory.resolve("persona-standardization-summary.csv"),
                    standardization
            );
            writeStandardizedResults(
                    outputDirectory.resolve("persona-standardized-results.csv"),
                    standardization
            );
        } catch (IOException exception) {
            throw new IllegalStateException("유형별 시뮬레이션 CSV를 저장하지 못했습니다.", exception);
        }
    }

    public void exportSeedStability(
            Path outputPath,
            List<PersonaProfileSimulationResult> results) {
        try {
            Files.createDirectories(outputPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                writer.write(BOM);
                writeRow(writer, List.of("난수_Seed(random_seed)", "목표_성향(target_persona)",
                        "일치율(accuracy_rate)"));
                results.stream().map(PersonaProfileSimulationResult::randomSeed).distinct()
                        .forEach(seed -> {
                            List<PersonaProfileSimulationResult> seedResults = results.stream()
                                    .filter(result -> result.randomSeed() == seed)
                                    .toList();
                            PersonaProfileSimulationAnalysis analysis =
                                    new PersonaProfileSimulationAnalysis(seedResults);
                            for (PersonaType personaType : PersonaType.values()) {
                                writeRowUnchecked(writer, List.of(
                                        String.valueOf(seed),
                                        personaType.name(),
                                        analysis.getAccuracy(personaType).toPlainString()
                                ));
                            }
                        });
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Seed 안정성 CSV를 저장하지 못했습니다.", exception);
        }
    }

    private void writeResults(
            Path outputPath,
            List<PersonaProfileSimulationResult> results) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(BOM);
            writeRow(writer, List.of(
                    "난수_Seed(random_seed)", "가상_사용자_ID(simulation_user_id)",
                    "목표_성향(target_persona)", "판정_성향(predicted_persona)",
                    "판정_일치(matches_target)", "RT_점수(rt_score)",
                    "LH_점수(lh_score)", "RP_점수(rp_score)"
            ));
            for (PersonaProfileSimulationResult result : results) {
                GameBehaviorSimulationResult simulation = result.simulationResult();
                writeRow(writer, List.of(
                        String.valueOf(result.randomSeed()),
                        String.valueOf(simulation.getSimulationUserId()),
                        result.targetPersona().name(),
                        result.predictedPersona().name(),
                        String.valueOf(result.matchesTarget()),
                        simulation.getFinalRtScore().toPlainString(),
                        simulation.getFinalLhScore().toPlainString(),
                        simulation.getFinalRpScore().toPlainString()
                ));
            }
        }
    }

    private void writeConfusionMatrix(
            Path outputPath,
            PersonaProfileSimulationAnalysis analysis) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(BOM);
            List<String> header = new ArrayList<>();
            header.add("목표_성향(target_persona)");
            for (PersonaType predicted : PersonaType.values()) {
                header.add("판정_" + predicted.name());
            }
            header.add("일치율(accuracy_rate)");
            writeRow(writer, header);
            for (PersonaType target : PersonaType.values()) {
                List<String> row = new ArrayList<>();
                row.add(target.name());
                for (PersonaType predicted : PersonaType.values()) {
                    row.add(String.valueOf(analysis.getCount(target, predicted)));
                }
                row.add(analysis.getAccuracy(target).toPlainString());
                writeRow(writer, row);
            }
        }
    }

    private void writeStandardizationSummary(
            Path outputPath,
            PersonaStandardizationAnalysis analysis) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(BOM);
            writeRow(writer, List.of(
                    "기준_안내(criteria_note)", "목표_성향(target_persona)",
                    "RT_평균(rt_average)", "RT_표준편차(rt_standard_deviation)",
                    "LH_평균(lh_average)", "LH_표준편차(lh_standard_deviation)",
                    "RP_평균(rp_average)", "RP_표준편차(rp_standard_deviation)"
            ));
            for (PersonaType personaType : PersonaType.values()) {
                PersonaStandardizationAnalysis.PersonaScoreStatistics statistics =
                        analysis.getStatistics(personaType);
                writeRow(writer, List.of(
                        "시뮬레이션 데이터 기준",
                        personaType.name(),
                        statistics.rtAverage().toPlainString(),
                        statistics.rtStandardDeviation().toPlainString(),
                        statistics.lhAverage().toPlainString(),
                        statistics.lhStandardDeviation().toPlainString(),
                        statistics.rpAverage().toPlainString(),
                        statistics.rpStandardDeviation().toPlainString()
                ));
            }
        }
    }

    private void writeStandardizedResults(
            Path outputPath,
            PersonaStandardizationAnalysis analysis) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(BOM);
            writeRow(writer, List.of(
                    "기준_안내(criteria_note)", "가상_사용자_ID(simulation_user_id)",
                    "목표_성향(target_persona)", "판정_성향(predicted_persona)",
                    "RT_Z점수(rt_z_score)", "LH_Z점수(lh_z_score)",
                    "RP_Z점수(rp_z_score)", "RT_백분위(rt_percentile)",
                    "LH_백분위(lh_percentile)", "RP_백분위(rp_percentile)",
                    "유형_평균점_거리(distance_from_persona_mean)",
                    "유형_유사도_백분위(similarity_percentile)"
            ));
            for (PersonaStandardizationAnalysis.StandardizedSimulationResult result
                    : analysis.getStandardizedResults()) {
                writeRow(writer, List.of(
                        "시뮬레이션 데이터 기준",
                        String.valueOf(result.source().simulationResult().getSimulationUserId()),
                        result.source().targetPersona().name(),
                        result.source().predictedPersona().name(),
                        result.rtZScore().toPlainString(),
                        result.lhZScore().toPlainString(),
                        result.rpZScore().toPlainString(),
                        result.rtPercentile().toPlainString(),
                        result.lhPercentile().toPlainString(),
                        result.rpPercentile().toPlainString(),
                        result.distanceFromPersonaMean().toPlainString(),
                        result.similarityPercentile().toPlainString()
                ));
            }
        }
    }

    private void writeRowUnchecked(BufferedWriter writer, List<String> values) {
        try {
            writeRow(writer, values);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void writeRow(BufferedWriter writer, List<String> values) throws IOException {
        writer.write(String.join(",", values));
        writer.newLine();
    }
}
