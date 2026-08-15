package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.service.ScenarioService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonaGameBehaviorGeneratorTest {

    @Test
    @DisplayName("유형별 생성기는 자산 범위 안에서 유효한 52 Tick 행동만 생성한다.")
    void generateValidPersonaBehavior() {
        ScenarioDto scenario = new ScenarioService().getScenario("SC001");
        PersonaBehaviorProfile profile = PersonaBehaviorProfiles.get(PersonaType.HHH);
        SimulatedGamePortfolio portfolio = new PersonaInitialPortfolioFactory().create(
                profile,
                scenario.getTicks().get(0).getPrice(),
                new java.util.SplittableRandom(20260816L)
        );

        GameBehaviorGenerationResult result = new PersonaGameBehaviorGenerator(profile)
                .generateGameBehavior(
                        scenario,
                        portfolio,
                        20260816L,
                        GameBehaviorFrequencyCondition.MEDIUM,
                        TradeQuantityGenerationCondition.SYMMETRIC_THREE_LEVEL
                );

        assertEquals(52, result.getDecisionTickCount());
        assertTrue(result.getActions().stream()
                .allMatch(action -> action.getGameTick() >= 0 && action.getGameTick() <= 52));
        assertTrue(result.getActions().stream()
                .allMatch(action -> action.getActionType() == BehaviorActionType.BUY
                        || action.getActionType() == BehaviorActionType.SELL
                        || action.getActionType() == BehaviorActionType.CANCEL_PRODUCT
                        || action.getActionType() == BehaviorActionType.MATURITY));
    }
}
