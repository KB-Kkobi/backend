package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.service.ScenarioService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeutralGameBehaviorGeneratorTest {

    private static final long RANDOM_SEED = 20260815L;

    private final ScenarioService scenarioService = new ScenarioService();
    private final NeutralGameBehaviorGenerator behaviorGenerator =
            new NeutralGameBehaviorGenerator();

    @Test
    @DisplayName("실제 52 Tick 시나리오에서 유효한 중립 행동을 생성한다.")
    void generateNeutralGameBehavior() {
        ScenarioDto scenario = scenarioService.getScenario("SC001");
        SimulatedGamePortfolio portfolio = createPortfolio();

        GameBehaviorGenerationResult result = behaviorGenerator.generateGameBehavior(
                scenario,
                portfolio,
                RANDOM_SEED
        );

        assertEquals(52, result.getDecisionTickCount());
        assertTrue(result.getNoActionTickCount() >= 0);
        assertTrue(result.getNoActionTickCount() <= result.getDecisionTickCount());
        assertFalse(result.getActions().isEmpty());
        assertTrue(result.getActions().stream()
                .allMatch(action -> action.getCurrentCash() >= 0));
        assertTrue(result.getActions().stream()
                .allMatch(action -> action.getCurrentStockPrincipal() >= 0));
        assertTrue(result.getActions().stream()
                .allMatch(action -> action.getCurrentDeposit() >= 0));
        assertTrue(result.getActions().stream()
                .filter(action -> action.getActionType() != BehaviorActionType.MATURITY)
                .allMatch(action -> action.getGameTick() >= 0 && action.getGameTick() < 52));
    }

    @Test
    @DisplayName("동일한 난수 Seed는 동일한 행동 순서를 생성한다.")
    void generateSameBehaviorWithSameRandomSeed() {
        ScenarioDto scenario = scenarioService.getScenario("SC001");

        GameBehaviorGenerationResult firstResult = behaviorGenerator.generateGameBehavior(
                scenario,
                createPortfolio(),
                RANDOM_SEED
        );
        GameBehaviorGenerationResult secondResult = behaviorGenerator.generateGameBehavior(
                scenario,
                createPortfolio(),
                RANDOM_SEED
        );

        assertEquals(
                createActionSignatures(firstResult.getActions()),
                createActionSignatures(secondResult.getActions())
        );
        assertEquals(firstResult.getNoActionTickCount(), secondResult.getNoActionTickCount());
    }

    @Test
    @DisplayName("여러 난수 Seed에서도 보유 자산을 초과하는 행동을 생성하지 않는다.")
    void generateValidBehaviorWithMultipleRandomSeeds() {
        ScenarioDto scenario = scenarioService.getScenario("SC001");

        for (long randomSeed = 1L; randomSeed <= 100L; randomSeed++) {
            GameBehaviorGenerationResult result = behaviorGenerator.generateGameBehavior(
                    scenario,
                    createPortfolio(),
                    randomSeed
            );

            assertTrue(result.getActions().stream()
                    .allMatch(action -> action.getCurrentCash() >= 0));
            assertTrue(result.getActions().stream()
                    .allMatch(action -> action.getCurrentStockPrincipal() >= 0));
            assertTrue(result.getActions().stream()
                    .allMatch(action -> action.getCurrentStockQuantity() >= 0));
        }
    }

    @Test
    @DisplayName("게임 종료 Tick이 없으면 행동을 생성할 수 없다.")
    void rejectScenarioWithoutCompletionTick() {
        ScenarioDto scenario = scenarioService.getScenario("SC001");
        scenario.setTicks(scenario.getTicks().stream()
                .filter(tick -> tick.getTick() < scenario.getTotalTicks())
                .toList());

        assertThrows(
                IllegalArgumentException.class,
                () -> behaviorGenerator.generateGameBehavior(
                        scenario,
                        createPortfolio(),
                        RANDOM_SEED
                )
        );
    }

    private SimulatedGamePortfolio createPortfolio() {
        return new SimulatedGamePortfolio(
                3_000_000L,
                4_000_000L,
                3_000_000L,
                200
        );
    }

    private List<String> createActionSignatures(List<SimulatedGameAction> actions) {
        return actions.stream()
                .map(action -> String.join(
                        ":",
                        String.valueOf(action.getGameTick()),
                        action.getActionType().name(),
                        String.valueOf(action.getActionAmount()),
                        String.valueOf(action.getQuantity())
                ))
                .toList();
    }
}
