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
    private static final int DEPOSIT_HOLD_PERCENTAGE = 50;

    public GameBehaviorGenerationResult generateGameBehavior(
            ScenarioDto scenario,
            SimulatedGamePortfolio portfolio,
            long randomSeed) {
        return generateGameBehavior(scenario, portfolio, randomSeed, null);
    }

    public GameBehaviorGenerationResult generateGameBehavior(
            ScenarioDto scenario,
            SimulatedGamePortfolio portfolio,
            long randomSeed,
            GameBehaviorFrequencyCondition frequencyCondition) {
        return generateGameBehavior(
                scenario,
                portfolio,
                randomSeed,
                frequencyCondition,
                TradeQuantityGenerationCondition.CURRENT_RANDOM_BUY_PERCENTAGE
        );
    }

    public GameBehaviorGenerationResult generateGameBehavior(
            ScenarioDto scenario,
            SimulatedGamePortfolio portfolio,
            long randomSeed,
            GameBehaviorFrequencyCondition frequencyCondition,
            TradeQuantityGenerationCondition tradeQuantityCondition) {
        if (tradeQuantityCondition == null) {
            throw new IllegalArgumentException("거래 수량 생성 조건은 필수입니다.");
        }
        List<ScenarioTickDto> decisionTicks = getDecisionTicks(scenario);
        SplittableRandom random = new SplittableRandom(randomSeed);
        List<SimulatedGameAction> actions = new ArrayList<>();
        int noActionTickCount = 0;
        DepositDecision depositDecision = createDepositDecision(
                portfolio,
                decisionTicks,
                random
        );

        for (ScenarioTickDto scenarioTick : decisionTicks) {
            int actionCount = applyDepositDecision(
                    scenarioTick,
                    portfolio,
                    actions,
                    depositDecision
            );
            actionCount += generateTickActions(
                    scenarioTick,
                    portfolio,
                    random,
                    actions,
                    frequencyCondition,
                    tradeQuantityCondition
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
            List<SimulatedGameAction> actions,
            GameBehaviorFrequencyCondition frequencyCondition,
            TradeQuantityGenerationCondition tradeQuantityCondition) {
        if (frequencyCondition == null) {
            return generateBaselineTickActions(
                    scenarioTick,
                    portfolio,
                    random,
                    actions,
                    tradeQuantityCondition
            );
        }
        if (!canApplyPercentage(random, frequencyCondition.getActionStartPercentage())) {
            return 0;
        }

        int actionCount = 0;
        while (actionCount < frequencyCondition.getMaximumActionCountPerTick()) {
            CandidateAction selectedAction = selectTradeAction(
                    scenarioTick.getPrice(),
                    portfolio,
                    random
            );
            if (selectedAction == null) {
                break;
            }

            actions.add(createAction(
                    selectedAction,
                    scenarioTick,
                    portfolio,
                    random,
                    tradeQuantityCondition
            ));
            actionCount++;
            if (actionCount >= frequencyCondition.getMaximumActionCountPerTick()
                    || !canApplyPercentage(
                    random,
                    frequencyCondition.getAdditionalActionPercentage()
            )) {
                break;
            }
        }
        return actionCount;
    }

    private int generateBaselineTickActions(
            ScenarioTickDto scenarioTick,
            SimulatedGamePortfolio portfolio,
            SplittableRandom random,
            List<SimulatedGameAction> actions,
            TradeQuantityGenerationCondition tradeQuantityCondition) {
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
                    random,
                    tradeQuantityCondition
            ));
            actionCount++;
        }
        return actionCount;
    }

    private CandidateAction selectTradeAction(
            long executionPrice,
            SimulatedGamePortfolio portfolio,
            SplittableRandom random) {
        EnumSet<CandidateAction> candidateActions = EnumSet.noneOf(CandidateAction.class);
        if (portfolio.canBuyStock(executionPrice)) {
            candidateActions.add(CandidateAction.BUY);
        }
        if (portfolio.canSellStock()) {
            candidateActions.add(CandidateAction.SELL);
        }
        if (candidateActions.isEmpty()) {
            return null;
        }

        List<CandidateAction> selectableActions = List.copyOf(candidateActions);
        return selectableActions.get(random.nextInt(selectableActions.size()));
    }

    private boolean canApplyPercentage(
            SplittableRandom random,
            int percentage) {
        return random.nextInt(100) < percentage;
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

        List<CandidateAction> selectableActions = List.copyOf(candidateActions);
        return selectableActions.get(random.nextInt(selectableActions.size()));
    }

    private SimulatedGameAction createAction(
            CandidateAction selectedAction,
            ScenarioTickDto scenarioTick,
            SimulatedGamePortfolio portfolio,
            SplittableRandom random,
            TradeQuantityGenerationCondition tradeQuantityCondition) {
        return switch (selectedAction) {
            case BUY -> portfolio.buyStock(
                    scenarioTick.getTick(),
                    generateBuyQuantity(
                            portfolio.getMaximumBuyQuantity(scenarioTick.getPrice()),
                            random,
                            tradeQuantityCondition
                    ),
                    scenarioTick.getPrice()
            );
            case SELL -> portfolio.sellStock(
                    scenarioTick.getTick(),
                    generateSellQuantity(portfolio.getCurrentStockQuantity(), random),
                    scenarioTick.getPrice()
            );
            case STOP -> throw new IllegalStateException("무행동은 거래 행동으로 생성할 수 없습니다.");
        };
    }

    private DepositDecision createDepositDecision(
            SimulatedGamePortfolio portfolio,
            List<ScenarioTickDto> decisionTicks,
            SplittableRandom random) {
        if (!portfolio.existsActiveDeposit()
                || canApplyPercentage(random, DEPOSIT_HOLD_PERCENTAGE)) {
            return DepositDecision.hold();
        }
        int cancellationTick = decisionTicks.get(
                random.nextInt(decisionTicks.size())
        ).getTick();
        return DepositDecision.cancelAt(cancellationTick);
    }

    private int applyDepositDecision(
            ScenarioTickDto scenarioTick,
            SimulatedGamePortfolio portfolio,
            List<SimulatedGameAction> actions,
            DepositDecision depositDecision) {
        if (!depositDecision.shouldCancelAt(scenarioTick.getTick())) {
            return 0;
        }
        actions.add(portfolio.cancelDeposit(scenarioTick.getTick()));
        return 1;
    }

    private int generateBuyQuantity(
            int maximumQuantity,
            SplittableRandom random,
            TradeQuantityGenerationCondition tradeQuantityCondition) {
        if (maximumQuantity <= 0) {
            throw new IllegalArgumentException("최대 거래 가능 수량은 0보다 커야 합니다.");
        }
        int investmentPercentage = random.nextInt(1, 101);
        if (tradeQuantityCondition
                == TradeQuantityGenerationCondition.SYMMETRIC_THREE_LEVEL) {
            return TradeQuantityType.fromPercentage(investmentPercentage)
                    .calculateQuantity(maximumQuantity);
        }
        return Math.max(
                1,
                (int) ((long) maximumQuantity * investmentPercentage / 100)
        );
    }

    private int generateSellQuantity(
            int currentStockQuantity,
            SplittableRandom random) {
        if (currentStockQuantity <= 0) {
            throw new IllegalArgumentException("보유 주식 수량은 0보다 커야 합니다.");
        }
        TradeQuantityType tradeQuantityType = TradeQuantityType.values()[
                random.nextInt(TradeQuantityType.values().length)
        ];
        return tradeQuantityType.calculateQuantity(currentStockQuantity);
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
        SELL
    }

    private enum TradeQuantityType {
        PARTIAL {
            @Override
            int calculateQuantity(int currentStockQuantity) {
                return Math.max(1, currentStockQuantity / 4);
            }
        },
        HALF {
            @Override
            int calculateQuantity(int currentStockQuantity) {
                return Math.max(1, currentStockQuantity / 2);
            }
        },
        FULL {
            @Override
            int calculateQuantity(int currentStockQuantity) {
                return currentStockQuantity;
            }
        };

        private static TradeQuantityType fromPercentage(int percentage) {
            if (percentage <= 33) {
                return PARTIAL;
            }
            if (percentage <= 66) {
                return HALF;
            }
            return FULL;
        }

        abstract int calculateQuantity(int currentStockQuantity);
    }

    private record DepositDecision(boolean cancellation, Integer cancellationTick) {

        private static DepositDecision hold() {
            return new DepositDecision(false, null);
        }

        private static DepositDecision cancelAt(int cancellationTick) {
            return new DepositDecision(true, cancellationTick);
        }

        private boolean shouldCancelAt(int gameTick) {
            return cancellation && cancellationTick == gameTick;
        }
    }
}
