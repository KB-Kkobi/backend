package org.kkobi.assessment.simulation;

import org.kkobi.game.dto.ScenarioDto;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public class PersonaProfileSimulationRunner {

    private final PersonaInitialPortfolioFactory portfolioFactory =
            new PersonaInitialPortfolioFactory();

    public List<PersonaProfileSimulationResult> run(
            ScenarioDto scenario,
            int userCountPerPersona,
            long randomSeed) {
        return run(
                scenario,
                userCountPerPersona,
                randomSeed,
                GameRuleEvaluationCondition.LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP
        );
    }

    public List<PersonaProfileSimulationResult> run(
            ScenarioDto scenario,
            int userCountPerPersona,
            long randomSeed,
            GameRuleEvaluationCondition ruleEvaluationCondition) {
        if (scenario == null || scenario.getTicks() == null) {
            throw new IllegalArgumentException("게임 시나리오는 필수입니다.");
        }
        if (userCountPerPersona <= 0) {
            throw new IllegalArgumentException("유형별 사용자 수는 0보다 커야 합니다.");
        }
        if (ruleEvaluationCondition == null) {
            throw new IllegalArgumentException("게임 규칙 평가 조건은 필수입니다.");
        }
        long initialStockPrice = scenario.getTicks().stream()
                .filter(tick -> tick.getTick() == 0)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("초기 Tick을 찾을 수 없습니다."))
                .getPrice();
        SplittableRandom random = new SplittableRandom(randomSeed);
        List<PersonaProfileSimulationResult> results = new ArrayList<>();
        long simulationUserId = 1L;

        for (PersonaBehaviorProfile profile : PersonaBehaviorProfiles.values()) {
            GameBehaviorSimulator simulator = new GameBehaviorSimulator(
                    new PersonaGameBehaviorGenerator(profile)
            );
            for (int index = 0; index < userCountPerPersona; index++) {
                SplittableRandom userRandom = random.split();
                GameBehaviorSimulationResult simulationResult = simulator.simulateGame(
                        simulationUserId++,
                        scenario,
                        portfolioFactory.create(profile, initialStockPrice, userRandom),
                        userRandom.nextLong(),
                        GameBehaviorFrequencyCondition.MEDIUM,
                        ConsecutiveActionMultiplierCondition.DISABLED,
                        SameTickRuleApplicationCondition.ONCE_PER_TICK,
                        RuleAccumulationCondition.UNLIMITED,
                        LossAveragingRtWeightCondition.RT_15,
                        ruleEvaluationCondition,
                        TradeQuantityGenerationCondition.SYMMETRIC_THREE_LEVEL
                );
                results.add(new PersonaProfileSimulationResult(
                        randomSeed,
                        profile.targetPersona(),
                        simulationResult
                ));
            }
        }
        return List.copyOf(results);
    }
}
