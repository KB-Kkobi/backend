package org.kkobi.assessment.simulation;

import org.kkobi.game.dto.ScenarioDto;

public interface GameBehaviorGenerator {

    GameBehaviorGenerationResult generateGameBehavior(
            ScenarioDto scenario,
            SimulatedGamePortfolio portfolio,
            long randomSeed,
            GameBehaviorFrequencyCondition frequencyCondition,
            TradeQuantityGenerationCondition tradeQuantityCondition);
}
