package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.PersonaType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PersonaProfileSimulationAnalysis {

    private static final BigDecimal PERCENTAGE = BigDecimal.valueOf(100);
    private final Map<PersonaType, Map<PersonaType, Integer>> confusionMatrix;
    private final Map<PersonaType, BigDecimal> accuracyByTarget;

    public PersonaProfileSimulationAnalysis(List<PersonaProfileSimulationResult> results) {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("분석할 시뮬레이션 결과는 필수입니다.");
        }
        this.confusionMatrix = createConfusionMatrix(results);
        this.accuracyByTarget = createAccuracyByTarget(confusionMatrix);
    }

    public int getCount(PersonaType target, PersonaType predicted) {
        return confusionMatrix.get(target).get(predicted);
    }

    public BigDecimal getAccuracy(PersonaType target) {
        return accuracyByTarget.get(target);
    }

    private Map<PersonaType, Map<PersonaType, Integer>> createConfusionMatrix(
            List<PersonaProfileSimulationResult> results) {
        EnumMap<PersonaType, Map<PersonaType, Integer>> matrix =
                new EnumMap<>(PersonaType.class);
        for (PersonaType target : PersonaType.values()) {
            EnumMap<PersonaType, Integer> predictedCounts = new EnumMap<>(PersonaType.class);
            for (PersonaType predicted : PersonaType.values()) {
                predictedCounts.put(predicted, 0);
            }
            matrix.put(target, predictedCounts);
        }
        for (PersonaProfileSimulationResult result : results) {
            Map<PersonaType, Integer> predictedCounts = matrix.get(result.targetPersona());
            predictedCounts.compute(result.predictedPersona(), (key, count) -> count + 1);
        }
        return Map.copyOf(matrix);
    }

    private Map<PersonaType, BigDecimal> createAccuracyByTarget(
            Map<PersonaType, Map<PersonaType, Integer>> matrix) {
        EnumMap<PersonaType, BigDecimal> accuracies = new EnumMap<>(PersonaType.class);
        matrix.forEach((target, predictedCounts) -> {
            int total = predictedCounts.values().stream().mapToInt(Integer::intValue).sum();
            int matched = predictedCounts.get(target);
            accuracies.put(
                    target,
                    BigDecimal.valueOf(matched)
                            .multiply(PERCENTAGE)
                            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
            );
        });
        return Map.copyOf(accuracies);
    }
}
