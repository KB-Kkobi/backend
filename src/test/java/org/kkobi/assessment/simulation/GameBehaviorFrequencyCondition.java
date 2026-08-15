package org.kkobi.assessment.simulation;

import lombok.Getter;

@Getter
public enum GameBehaviorFrequencyCondition {

    LOW("저빈도", 15, 0, 1),
    MEDIUM("중간 빈도", 50, 20, 2),
    HIGH("고빈도", 85, 70, 2);

    private final String description;
    private final int actionStartPercentage;
    private final int additionalActionPercentage;
    private final int maximumActionCountPerTick;

    GameBehaviorFrequencyCondition(
            String description,
            int actionStartPercentage,
            int additionalActionPercentage,
            int maximumActionCountPerTick) {
        this.description = description;
        this.actionStartPercentage = actionStartPercentage;
        this.additionalActionPercentage = additionalActionPercentage;
        this.maximumActionCountPerTick = maximumActionCountPerTick;
    }
}
