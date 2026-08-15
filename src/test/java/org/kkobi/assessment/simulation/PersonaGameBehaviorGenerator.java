package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.MarketState;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

public class PersonaGameBehaviorGenerator implements GameBehaviorGenerator {

    private final PersonaBehaviorProfile profile;

    public PersonaGameBehaviorGenerator(PersonaBehaviorProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("성향별 행동 프로필은 필수입니다.");
        }
        this.profile = profile;
    }

    @Override
    public GameBehaviorGenerationResult generateGameBehavior(
            ScenarioDto scenario,
            SimulatedGamePortfolio portfolio,
            long randomSeed,
            GameBehaviorFrequencyCondition frequencyCondition,
            TradeQuantityGenerationCondition tradeQuantityCondition) {
        validateInput(scenario, portfolio, tradeQuantityCondition);
        SplittableRandom random = new SplittableRandom(randomSeed);
        List<ScenarioTickDto> decisionTicks = scenario.getTicks().stream()
                .filter(tick -> tick.getTick() >= 0 && tick.getTick() < scenario.getTotalTicks())
                .sorted(Comparator.comparingInt(ScenarioTickDto::getTick))
                .toList();
        if (decisionTicks.size() != scenario.getTotalTicks()) {
            throw new IllegalArgumentException("행동 생성에 필요한 시나리오 Tick이 부족합니다.");
        }

        List<SimulatedGameAction> actions = new ArrayList<>();
        Integer depositCancelTick = createDepositCancelTick(decisionTicks, portfolio, random);
        int noActionTickCount = 0;

        for (ScenarioTickDto tick : decisionTicks) {
            boolean acted = false;
            if (depositCancelTick != null && depositCancelTick == tick.getTick()) {
                actions.add(portfolio.cancelDeposit(tick.getTick()));
                acted = true;
            }
            if (canApply(random, profile.actionProbability())) {
                SimulatedGameAction trade = createTrade(tick, portfolio, random);
                if (trade != null) {
                    actions.add(trade);
                    acted = true;
                }
            }
            if (!acted) {
                noActionTickCount++;
            }
        }

        if (portfolio.existsActiveDeposit()) {
            actions.add(portfolio.matureDeposit(scenario.getTotalTicks(), 0L));
        }
        return new GameBehaviorGenerationResult(actions, decisionTicks.size(), noActionTickCount);
    }

    private Integer createDepositCancelTick(
            List<ScenarioTickDto> ticks,
            SimulatedGamePortfolio portfolio,
            SplittableRandom random) {
        if (!portfolio.existsActiveDeposit()
                || !canApply(random, profile.depositCancelProbability())) {
            return null;
        }
        return ticks.get(random.nextInt(ticks.size())).getTick();
    }

    private SimulatedGameAction createTrade(
            ScenarioTickDto tick,
            SimulatedGamePortfolio portfolio,
            SplittableRandom random) {
        MarketState marketState = calculateMarketState(tick.getChangeRate());
        int buyProbability = getBuyProbability(marketState);
        int sellProbability = getSellProbability(marketState);
        boolean canBuy = portfolio.canBuyStock(tick.getPrice());
        boolean canSell = portfolio.canSellStock();
        if (!canBuy && !canSell) {
            return null;
        }

        int buyWeight = canBuy ? buyProbability : 0;
        int sellWeight = canSell ? sellProbability : 0;
        if (buyWeight + sellWeight == 0) {
            return null;
        }
        if (random.nextInt(buyWeight + sellWeight) < buyWeight) {
            int maximumQuantity = portfolio.getMaximumBuyQuantity(tick.getPrice());
            return portfolio.buyStock(
                    tick.getTick(),
                    createQuantity(maximumQuantity, random),
                    tick.getPrice()
            );
        }
        return portfolio.sellStock(
                tick.getTick(),
                createQuantity(portfolio.getCurrentStockQuantity(), random),
                tick.getPrice()
        );
    }

    private int createQuantity(int maximumQuantity, SplittableRandom random) {
        int[] percentages = {25, 50, 100};
        int percentage = percentages[random.nextInt(percentages.length)];
        return Math.max(1, (int) ((long) maximumQuantity * percentage / 100));
    }

    private MarketState calculateMarketState(double changeRate) {
        if (changeRate <= -5) {
            return MarketState.CRASH;
        }
        if (changeRate >= 3) {
            return MarketState.BULL;
        }
        return MarketState.NORMAL;
    }

    private int getBuyProbability(MarketState marketState) {
        return switch (marketState) {
            case CRASH -> profile.crashBuyProbability();
            case BULL -> profile.bullBuyProbability();
            case NORMAL, VOLATILE -> profile.normalBuyProbability();
        };
    }

    private int getSellProbability(MarketState marketState) {
        return switch (marketState) {
            case CRASH -> profile.crashSellProbability();
            case BULL -> profile.bullSellProbability();
            case NORMAL, VOLATILE -> profile.normalSellProbability();
        };
    }

    private boolean canApply(SplittableRandom random, int probability) {
        return random.nextInt(100) < probability;
    }

    private void validateInput(
            ScenarioDto scenario,
            SimulatedGamePortfolio portfolio,
            TradeQuantityGenerationCondition tradeQuantityCondition) {
        if (scenario == null || scenario.getTicks() == null) {
            throw new IllegalArgumentException("게임 시나리오는 필수입니다.");
        }
        if (portfolio == null) {
            throw new IllegalArgumentException("가상 사용자 자산은 필수입니다.");
        }
        if (tradeQuantityCondition == null) {
            throw new IllegalArgumentException("거래 수량 생성 조건은 필수입니다.");
        }
    }
}
