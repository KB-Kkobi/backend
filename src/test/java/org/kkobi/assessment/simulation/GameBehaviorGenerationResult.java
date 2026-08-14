package org.kkobi.assessment.simulation;

import lombok.Getter;
import org.kkobi.assessment.enums.BehaviorActionType;

import java.util.List;

@Getter
public class GameBehaviorGenerationResult {

    private final List<SimulatedGameAction> actions;
    private final int decisionTickCount;
    private final int noActionTickCount;

    public GameBehaviorGenerationResult(
            List<SimulatedGameAction> actions,
            int decisionTickCount,
            int noActionTickCount) {
        if (decisionTickCount < 0) {
            throw new IllegalArgumentException("행동 결정 Tick 수는 0 이상이어야 합니다.");
        }
        if (noActionTickCount < 0 || noActionTickCount > decisionTickCount) {
            throw new IllegalArgumentException("무행동 Tick 수가 행동 결정 Tick 범위를 벗어났습니다.");
        }
        this.actions = actions == null ? List.of() : List.copyOf(actions);
        this.decisionTickCount = decisionTickCount;
        this.noActionTickCount = noActionTickCount;
    }

    public int getActedTickCount() {
        return decisionTickCount - noActionTickCount;
    }

    public long getUserActionCount() {
        return actions.stream()
                .filter(action -> action.getActionType() != BehaviorActionType.MATURITY)
                .count();
    }
}
