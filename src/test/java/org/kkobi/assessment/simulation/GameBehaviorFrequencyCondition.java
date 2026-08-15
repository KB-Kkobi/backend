package org.kkobi.assessment.simulation;

import lombok.Getter;

@Getter
public enum GameBehaviorFrequencyCondition {

    LOW("저빈도 중립", 15, 0, 1, false),
    MEDIUM("중간 빈도 중립", 50, 20, 2, false),
    HIGH("고빈도 스트레스", 85, 70, 2, true);

    private final String description;
    private final int actionStartPercentage;
    private final int additionalActionPercentage;
    private final int maximumActionCountPerTick;
    private final boolean stressTest;

    GameBehaviorFrequencyCondition(
            String description,
            int actionStartPercentage,
            int additionalActionPercentage,
            int maximumActionCountPerTick,
            boolean stressTest) {
        this.description = description;
        this.actionStartPercentage = actionStartPercentage;
        this.additionalActionPercentage = additionalActionPercentage;
        this.maximumActionCountPerTick = maximumActionCountPerTick;
        this.stressTest = stressTest;
    }

    public String getValidationType() {
        return stressTest ? "STRESS_TEST" : "NEUTRAL_VALIDATION";
    }
}
