package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.MarketState;
import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

public class PersonaGameBehaviorGenerator implements GameBehaviorGenerator {

    private static final int DEPOSIT_CASH_RETENTION_TICKS = 2;
    private static final int CASH_BUFFER_MAINTENANCE_TICKS = 3;
    private static final BigDecimal CASH_BUFFER_MINIMUM_RATIO = BigDecimal.valueOf(25);
    private static final BigDecimal CASH_BUFFER_MAXIMUM_RATIO = BigDecimal.valueOf(50);

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
        TargetedOpportunity lhhOpportunity = createLhhOpportunity(decisionTicks);
        Integer depositCancelTick = createDepositCancelTick(decisionTicks, portfolio, random);
        boolean retainCashAfterDepositCancel = depositCancelTick != null
                && canApply(random, profile.depositCashRetentionProbability());
        boolean maintainCashBuffer = canApply(
                random,
                profile.cashBufferMaintenanceProbability()
        );
        Integer depositCashRetentionEndTick = null;
        int cashBufferMaintenanceTicksRemaining = 0;
        boolean cashBufferMaintenanceCompleted = false;
        boolean hhlNoChaseStarted = false;
        boolean hhlNoChaseCompleted = false;
        int noActionTickCount = 0;

        for (ScenarioTickDto tick : decisionTicks) {
            boolean acted = false;
            MarketState marketState = calculateMarketState(tick.getChangeRate());
            if (lhhOpportunity != null
                    && lhhOpportunity.buyTicks().contains(tick.getTick())) {
                SimulatedGameAction plannedBuy = createTargetedLhhBuy(tick, portfolio);
                if (plannedBuy != null) {
                    actions.add(plannedBuy);
                    acted = true;
                }
                if (!acted) {
                    noActionTickCount++;
                }
                continue;
            }
            if (lhhOpportunity != null
                    && lhhOpportunity.sellTicks().contains(tick.getTick())) {
                SimulatedGameAction profitSell = createTargetedLhhProfitSell(tick, portfolio);
                if (profitSell != null) {
                    actions.add(profitSell);
                    acted = true;
                }
                if (!acted) {
                    noActionTickCount++;
                }
                continue;
            }
            if (depositCancelTick != null && depositCancelTick == tick.getTick()) {
                actions.add(portfolio.cancelDeposit(tick.getTick()));
                acted = true;
                if (retainCashAfterDepositCancel) {
                    depositCashRetentionEndTick = tick.getTick()
                            + DEPOSIT_CASH_RETENTION_TICKS;
                } else if (portfolio.canBuyStock(tick.getPrice())
                        && canApply(random, profile.depositCancelThenBuyProbability())) {
                    actions.add(createBuy(tick, portfolio, random));
                }
                continue;
            }
            if (depositCashRetentionEndTick != null
                    && tick.getTick() <= depositCashRetentionEndTick) {
                noActionTickCount++;
                continue;
            }
            if (maintainCashBuffer && !cashBufferMaintenanceCompleted) {
                if (cashBufferMaintenanceTicksRemaining == 0
                        && isCashBufferRatio(portfolio)) {
                    cashBufferMaintenanceTicksRemaining = CASH_BUFFER_MAINTENANCE_TICKS;
                }
                if (cashBufferMaintenanceTicksRemaining > 0) {
                    cashBufferMaintenanceTicksRemaining--;
                    cashBufferMaintenanceCompleted = cashBufferMaintenanceTicksRemaining == 0;
                    noActionTickCount++;
                    continue;
                }
            }
            if (profile.targetPersona() == PersonaType.HHL
                    && cashBufferMaintenanceCompleted
                    && !hhlNoChaseCompleted) {
                if (marketState == MarketState.BULL) {
                    hhlNoChaseStarted = true;
                    noActionTickCount++;
                    continue;
                }
                if (hhlNoChaseStarted) {
                    hhlNoChaseCompleted = true;
                } else {
                    noActionTickCount++;
                    continue;
                }
            }
            if (profile.targetPersona() == PersonaType.HLL
                    && marketState == MarketState.BULL) {
                noActionTickCount++;
                continue;
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

    private TargetedOpportunity createLhhOpportunity(List<ScenarioTickDto> ticks) {
        if (profile.targetPersona() != PersonaType.LHH) {
            return null;
        }
        List<Integer> buyTicks = new ArrayList<>();
        List<Integer> sellTicks = new ArrayList<>();
        int lastSellTick = -1;
        for (int buyIndex = 0; buyIndex < ticks.size(); buyIndex++) {
            ScenarioTickDto buyTick = ticks.get(buyIndex);
            if (buyTick.getTick() <= lastSellTick
                    || calculateMarketState(buyTick.getChangeRate()) != MarketState.NORMAL) {
                continue;
            }
            for (int sellIndex = buyIndex + 1; sellIndex < ticks.size(); sellIndex++) {
                ScenarioTickDto sellTick = ticks.get(sellIndex);
                if (sellTick.getTick() > buyTick.getTick() + 10) {
                    break;
                }
                if (sellTick.getPrice() > buyTick.getPrice()) {
                    buyTicks.add(buyTick.getTick());
                    sellTicks.add(sellTick.getTick());
                    lastSellTick = sellTick.getTick();
                    break;
                }
            }
            if (buyTicks.size() >= 3) {
                break;
            }
        }
        return buyTicks.isEmpty() ? null : new TargetedOpportunity(buyTicks, sellTicks);
    }

    private SimulatedGameAction createTargetedLhhBuy(
            ScenarioTickDto tick,
            SimulatedGamePortfolio portfolio) {
        if (!portfolio.canBuyStock(tick.getPrice())) {
            return null;
        }
        long targetAmount = portfolio.getCurrentTotalAssetPrincipal() / 10;
        long targetQuantity = Math.max(
                1,
                (targetAmount + tick.getPrice() - 1) / tick.getPrice()
        );
        int quantity = (int) Math.min(
                targetQuantity,
                portfolio.getMaximumBuyQuantity(tick.getPrice())
        );
        return quantity > 0
                ? portfolio.buyStock(tick.getTick(), quantity, tick.getPrice())
                : null;
    }

    private SimulatedGameAction createTargetedLhhProfitSell(
            ScenarioTickDto tick,
            SimulatedGamePortfolio portfolio) {
        if (!portfolio.canSellStock()) {
            return null;
        }
        int quantity = Math.max(1, portfolio.getCurrentStockQuantity() / 2);
        return portfolio.sellStock(tick.getTick(), quantity, tick.getPrice());
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
        if (marketState == MarketState.CRASH
                && portfolio.canSellStock()
                && canApply(random, profile.crashHoldingProbability())) {
            return null;
        }
        BigDecimal returnRate = calculateReturnRate(tick.getPrice(), portfolio);
        int buyProbability = getBuyProbability(marketState, returnRate);
        int sellProbability = getSellProbability(marketState, returnRate);
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
            return createBuy(tick, portfolio, random);
        }
        return portfolio.sellStock(
                tick.getTick(),
                createQuantity(portfolio.getCurrentStockQuantity(), random),
                tick.getPrice()
        );
    }

    private SimulatedGameAction createBuy(
            ScenarioTickDto tick,
            SimulatedGamePortfolio portfolio,
            SplittableRandom random) {
        return portfolio.buyStock(
                tick.getTick(),
                createQuantity(portfolio.getMaximumBuyQuantity(tick.getPrice()), random),
                tick.getPrice()
        );
    }

    private int createQuantity(int maximumQuantity, SplittableRandom random) {
        int selected = random.nextInt(100);
        int percentage;
        if (selected < profile.smallTradeProbability()) {
            percentage = 10;
        } else if (selected < profile.smallTradeProbability()
                + profile.mediumTradeProbability()) {
            percentage = 50;
        } else {
            percentage = 100;
        }
        return Math.max(1, (int) ((long) maximumQuantity * percentage / 100));
    }

    private BigDecimal calculateReturnRate(
            long currentPrice,
            SimulatedGamePortfolio portfolio) {
        BigDecimal averagePrice = portfolio.getAveragePurchasePrice();
        if (averagePrice == null || averagePrice.signum() == 0) {
            return null;
        }
        return BigDecimal.valueOf(currentPrice)
                .subtract(averagePrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(averagePrice, 2, java.math.RoundingMode.HALF_UP);
    }

    private boolean isCashBufferRatio(SimulatedGamePortfolio portfolio) {
        long totalAssetPrincipal = portfolio.getCurrentTotalAssetPrincipal();
        if (totalAssetPrincipal <= 0) {
            return false;
        }
        BigDecimal cashRatio = BigDecimal.valueOf(portfolio.getCurrentCash())
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(totalAssetPrincipal),
                        4,
                        java.math.RoundingMode.HALF_UP
                );
        return cashRatio.compareTo(CASH_BUFFER_MINIMUM_RATIO) >= 0
                && cashRatio.compareTo(CASH_BUFFER_MAXIMUM_RATIO) < 0;
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

    private int getBuyProbability(MarketState marketState, BigDecimal returnRate) {
        if (returnRate != null && returnRate.compareTo(BigDecimal.valueOf(-10)) <= 0) {
            return profile.lossAveragingProbability();
        }
        return switch (marketState) {
            case CRASH -> profile.crashBuyProbability();
            case BULL -> profile.bullBuyProbability();
            case NORMAL, VOLATILE -> profile.normalBuyProbability();
        };
    }

    private int getSellProbability(MarketState marketState, BigDecimal returnRate) {
        if (returnRate != null && returnRate.compareTo(BigDecimal.valueOf(-10)) <= 0) {
            return profile.lossCutProbability();
        }
        if (returnRate != null && returnRate.signum() > 0) {
            return profile.profitTakingProbability();
        }
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

    private record TargetedOpportunity(
            List<Integer> buyTicks,
            List<Integer> sellTicks) {
    }
}
