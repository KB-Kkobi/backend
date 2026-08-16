package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.PersonaType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PersonaStandardizationAnalysis {

    private static final int SCALE = 4;
    private final Map<PersonaType, PersonaScoreStatistics> statisticsByPersona;
    private final List<StandardizedSimulationResult> standardizedResults;

    public PersonaStandardizationAnalysis(List<PersonaProfileSimulationResult> results) {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("표준화할 시뮬레이션 결과는 필수입니다.");
        }
        this.statisticsByPersona = calculateStatistics(results);
        this.standardizedResults = calculateStandardizedResults(results, statisticsByPersona);
    }

    public PersonaScoreStatistics getStatistics(PersonaType personaType) {
        return statisticsByPersona.get(personaType);
    }

    public List<StandardizedSimulationResult> getStandardizedResults() {
        return standardizedResults;
    }

    private Map<PersonaType, PersonaScoreStatistics> calculateStatistics(
            List<PersonaProfileSimulationResult> results) {
        EnumMap<PersonaType, PersonaScoreStatistics> statistics =
                new EnumMap<>(PersonaType.class);
        for (PersonaType personaType : PersonaType.values()) {
            List<PersonaProfileSimulationResult> personaResults = results.stream()
                    .filter(result -> result.targetPersona() == personaType)
                    .toList();
            if (personaResults.isEmpty()) {
                throw new IllegalArgumentException(personaType + " 유형 결과가 없습니다.");
            }
            statistics.put(personaType, new PersonaScoreStatistics(
                    average(personaResults, ScoreAxis.RT),
                    standardDeviation(personaResults, ScoreAxis.RT),
                    average(personaResults, ScoreAxis.LH),
                    standardDeviation(personaResults, ScoreAxis.LH),
                    average(personaResults, ScoreAxis.RP),
                    standardDeviation(personaResults, ScoreAxis.RP)
            ));
        }
        return Map.copyOf(statistics);
    }

    private List<StandardizedSimulationResult> calculateStandardizedResults(
            List<PersonaProfileSimulationResult> results,
            Map<PersonaType, PersonaScoreStatistics> statistics) {
        List<MutableStandardizedResult> mutableResults = new ArrayList<>();
        for (PersonaProfileSimulationResult result : results) {
            PersonaScoreStatistics scoreStatistics = statistics.get(result.targetPersona());
            GameBehaviorSimulationResult simulation = result.simulationResult();
            mutableResults.add(new MutableStandardizedResult(
                    result,
                    zScore(simulation.getFinalRtScore(), scoreStatistics.rtAverage(),
                            scoreStatistics.rtStandardDeviation()),
                    zScore(simulation.getFinalLhScore(), scoreStatistics.lhAverage(),
                            scoreStatistics.lhStandardDeviation()),
                    zScore(simulation.getFinalRpScore(), scoreStatistics.rpAverage(),
                            scoreStatistics.rpStandardDeviation()),
                    distance(simulation, scoreStatistics)
            ));
        }

        List<StandardizedSimulationResult> finalized = new ArrayList<>();
        for (MutableStandardizedResult result : mutableResults) {
            List<MutableStandardizedResult> samePersona = mutableResults.stream()
                    .filter(candidate -> candidate.source().targetPersona()
                            == result.source().targetPersona())
                    .toList();
            GameBehaviorSimulationResult simulation = result.source().simulationResult();
            finalized.add(new StandardizedSimulationResult(
                    result.source(),
                    result.rtZScore(),
                    result.lhZScore(),
                    result.rpZScore(),
                    percentile(samePersona, simulation.getFinalRtScore(), ScoreAxis.RT),
                    percentile(samePersona, simulation.getFinalLhScore(), ScoreAxis.LH),
                    percentile(samePersona, simulation.getFinalRpScore(), ScoreAxis.RP),
                    result.distanceFromPersonaMean(),
                    similarityPercentile(samePersona, result.distanceFromPersonaMean())
            ));
        }
        return List.copyOf(finalized);
    }

    private BigDecimal average(
            List<PersonaProfileSimulationResult> results,
            ScoreAxis scoreAxis) {
        BigDecimal sum = results.stream()
                .map(result -> scoreAxis.get(result.simulationResult()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(results.size()), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal standardDeviation(
            List<PersonaProfileSimulationResult> results,
            ScoreAxis scoreAxis) {
        BigDecimal average = average(results, scoreAxis);
        double variance = results.stream()
                .map(result -> scoreAxis.get(result.simulationResult()))
                .mapToDouble(score -> Math.pow(score.subtract(average).doubleValue(), 2))
                .average()
                .orElse(0);
        return decimal(Math.sqrt(variance));
    }

    private BigDecimal zScore(
            BigDecimal score,
            BigDecimal average,
            BigDecimal standardDeviation) {
        if (standardDeviation.signum() == 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return score.subtract(average)
                .divide(standardDeviation, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal distance(
            GameBehaviorSimulationResult result,
            PersonaScoreStatistics statistics) {
        double rt = result.getFinalRtScore().subtract(statistics.rtAverage()).doubleValue();
        double lh = result.getFinalLhScore().subtract(statistics.lhAverage()).doubleValue();
        double rp = result.getFinalRpScore().subtract(statistics.rpAverage()).doubleValue();
        return decimal(Math.sqrt(rt * rt + lh * lh + rp * rp));
    }

    private BigDecimal percentile(
            List<MutableStandardizedResult> results,
            BigDecimal score,
            ScoreAxis scoreAxis) {
        long count = results.stream()
                .map(result -> scoreAxis.get(result.source().simulationResult()))
                .filter(candidate -> candidate.compareTo(score) <= 0)
                .count();
        return percentage(count, results.size());
    }

    private BigDecimal similarityPercentile(
            List<MutableStandardizedResult> results,
            BigDecimal distance) {
        long count = results.stream()
                .filter(candidate -> candidate.distanceFromPersonaMean().compareTo(distance) >= 0)
                .count();
        return percentage(count, results.size());
    }

    private BigDecimal percentage(long count, int total) {
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public record PersonaScoreStatistics(
            BigDecimal rtAverage,
            BigDecimal rtStandardDeviation,
            BigDecimal lhAverage,
            BigDecimal lhStandardDeviation,
            BigDecimal rpAverage,
            BigDecimal rpStandardDeviation) {
    }

    public record StandardizedSimulationResult(
            PersonaProfileSimulationResult source,
            BigDecimal rtZScore,
            BigDecimal lhZScore,
            BigDecimal rpZScore,
            BigDecimal rtPercentile,
            BigDecimal lhPercentile,
            BigDecimal rpPercentile,
            BigDecimal distanceFromPersonaMean,
            BigDecimal similarityPercentile) {
    }

    private record MutableStandardizedResult(
            PersonaProfileSimulationResult source,
            BigDecimal rtZScore,
            BigDecimal lhZScore,
            BigDecimal rpZScore,
            BigDecimal distanceFromPersonaMean) {
    }

    private enum ScoreAxis {
        RT {
            @Override
            BigDecimal get(GameBehaviorSimulationResult result) {
                return result.getFinalRtScore();
            }
        },
        LH {
            @Override
            BigDecimal get(GameBehaviorSimulationResult result) {
                return result.getFinalLhScore();
            }
        },
        RP {
            @Override
            BigDecimal get(GameBehaviorSimulationResult result) {
                return result.getFinalRpScore();
            }
        };

        abstract BigDecimal get(GameBehaviorSimulationResult result);
    }
}
