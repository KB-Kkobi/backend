package org.kkobi.assessment.simulation;

import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

public class NeutralGameBehaviorGenerator {

    private static final int MAXIMUM_ACTION_COUNT_PER_TICK = 2;

    public GameBehaviorGenerationResult generateGameBehavior(
            ScenarioDto scenario,
            SimulatedGamePortfolio portfolio,
            long randomSeed) {
        List<ScenarioTickDto> decisionTicks = getDecisionTicks(scenario);
        SplittableRandom random = new SplittableRandom(randomSeed);
        List<SimulatedGameAction> actions = new ArrayList<>();
        int noActionTickCount = 0;

        for (ScenarioTickDto scenarioTick : decisionTicks) {
            int actionCount = generateTickActions(
                    scenarioTick,
                    portfolio,
                    random,
                    actions
            );
            if (actionCount == 0) {
                noActionTickCount++;
            }
        }

        if (portfolio.existsActiveDeposit()) {
            actions.add(portfolio.matureDeposit(scenario.getTotalTicks(), 0L));
        }

        return new GameBehaviorGenerationResult(
                actions,
                decisionTicks.size(),
                noActionTickCount
        );
    }

    private int generateTickActions(
            ScenarioTickDto scenarioTick,
            SimulatedGamePortfolio portfolio,
            SplittableRandom random,
            List<SimulatedGameAction> actions) {
        int actionCount = 0;

        while (actionCount < MAXIMUM_ACTION_COUNT_PER_TICK) {
            CandidateAction selectedAction = selectCandidateAction(
                    scenarioTick.getPrice(),
                    portfolio,
                    random
            );
            if (selectedAction == CandidateAction.STOP) {
                break;
            }

            actions.add(createAction(
                    selectedAction,
                    scenarioTick,
                    portfolio,
                    random
            ));
            actionCount++;
        }
        return actionCount;
    }

    private CandidateAction selectCandidateAction(
            long executionPrice,
            SimulatedGamePortfolio portfolio,
            SplittableRandom random) {
        EnumSet<CandidateAction> candidateActions = EnumSet.of(CandidateAction.STOP);
        if (portfolio.canBuyStock(executionPrice)) {
            candidateActions.add(CandidateAction.BUY);
        }
        if (portfolio.canSellStock()) {
            candidateActions.add(CandidateAction.SELL);
        }
        if (portfolio.canCancelDeposit()) {
            candidateActions.add(CandidateAction.CANCEL_DEPOSIT);
        }

        List<CandidateAction> selectableActions = List.copyOf(candidateActions);
        return selectableActions.get(random.nextInt(selectableActions.size()));
    }

    private SimulatedGameAction createAction(
            CandidateAction selectedAction,
            ScenarioTickDto scenarioTick,
            SimulatedGamePortfolio portfolio,
            SplittableRandom random) {
        return switch (selectedAction) {
            case BUY -> portfolio.buyStock(
                    scenarioTick.getTick(),
                    generateQuantity(
                            portfolio.getMaximumBuyQuantity(scenarioTick.getPrice()),
                            random
                    ),
                    scenarioTick.getPrice()
            );
            case SELL -> portfolio.sellStock(
                    scenarioTick.getTick(),
                    generateQuantity(portfolio.getCurrentStockQuantity(), random),
                    scenarioTick.getPrice()
            );
            case CANCEL_DEPOSIT -> portfolio.cancelDeposit(scenarioTick.getTick());
            case STOP -> throw new IllegalStateException("무행동은 거래 행동으로 생성할 수 없습니다.");
        };
    }

    private int generateQuantity(
            int maximumQuantity,
            SplittableRandom random) {
        if (maximumQuantity <= 0) {
            throw new IllegalArgumentException("최대 거래 가능 수량은 0보다 커야 합니다.");
        }
        int investmentPercentage = random.nextInt(1, 101);
        return Math.max(
                1,
                (int) ((long) maximumQuantity * investmentPercentage / 100)
        );
    }

    private List<ScenarioTickDto> getDecisionTicks(ScenarioDto scenario) {
        validateScenario(scenario);
        List<ScenarioTickDto> decisionTicks = scenario.getTicks()
                .stream()
                .filter(tick -> tick.getTick() >= 0)
                .filter(tick -> tick.getTick() < scenario.getTotalTicks())
                .sorted(Comparator.comparingInt(ScenarioTickDto::getTick))
                .toList();

        if (decisionTicks.size() != scenario.getTotalTicks()) {
            throw new IllegalArgumentException("행동 생성에 필요한 시나리오 Tick이 연속적으로 존재해야 합니다.");
        }
        Set<Integer> tickNumbers = new HashSet<>();
        for (ScenarioTickDto tick : decisionTicks) {
            if (!tickNumbers.add(tick.getTick())) {
                throw new IllegalArgumentException("시나리오 Tick 번호는 중복될 수 없습니다.");
            }
            if (tick.getTick() != tickNumbers.size() - 1) {
                throw new IllegalArgumentException("시나리오 Tick은 0부터 순서대로 존재해야 합니다.");
            }
            if (tick.getPrice() <= 0) {
                throw new IllegalArgumentException("시나리오 주가는 0보다 커야 합니다.");
            }
        }
        boolean existsCompletionTick = scenario.getTicks()
                .stream()
                .anyMatch(tick -> tick.getTick() == scenario.getTotalTicks());
        if (!existsCompletionTick) {
            throw new IllegalArgumentException("게임 종료 Tick을 찾을 수 없습니다.");
        }
        return decisionTicks;
    }

    private void validateScenario(ScenarioDto scenario) {
        if (scenario == null) {
            throw new IllegalArgumentException("게임 시나리오는 필수입니다.");
        }
        if (scenario.getTotalTicks() <= 0) {
            throw new IllegalArgumentException("전체 게임 Tick 수는 0보다 커야 합니다.");
        }
        if (scenario.getTicks() == null || scenario.getTicks().isEmpty()) {
            throw new IllegalArgumentException("게임 시나리오 Tick 정보는 필수입니다.");
        }
    }

    private enum CandidateAction {
        STOP,
        BUY,
        SELL,
        CANCEL_DEPOSIT
    }
}
