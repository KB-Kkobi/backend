package org.kkobi.game.calculator;

import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Component
public class GameSecurityReturnCalculator {

    private static final BigDecimal PERCENTAGE = BigDecimal.valueOf(100);
    private static final int QUANTITY_SCALE = 12;
    private static final int PRICE_SCALE = 8;
    private static final int RETURN_RATE_SCALE = 2;

    public BigDecimal calculateCurrentReturnRate(
            ScenarioDto scenario,
            int gameTick,
            List<ActionLogDto> actionLogs) {
        BigDecimal averagePurchasePrice = calculateAveragePurchasePrice(scenario, actionLogs);
        if (averagePurchasePrice == null || averagePurchasePrice.signum() == 0) {
            return null;
        }

        BigDecimal currentPrice = getScenarioPrice(scenario, gameTick);
        return currentPrice.subtract(averagePurchasePrice)
                .multiply(PERCENTAGE)
                .divide(averagePurchasePrice, RETURN_RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAveragePurchasePrice(
            ScenarioDto scenario,
            List<ActionLogDto> actionLogs) {
        BigDecimal averagePurchasePrice = null;
        long previousStockPrincipal = 0L;
        List<ActionLogDto> sortedActionLogs = actionLogs.stream()
                .filter(actionLog -> actionLog.getGameTick() != null)
                .filter(actionLog -> actionLog.getCurrentStock() != null)
                .sorted(Comparator.comparing(ActionLogDto::getGameTick)
                        .thenComparing(
                                ActionLogDto::getActionLogId,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ))
                .toList();

        for (ActionLogDto actionLog : sortedActionLogs) {
            long currentStockPrincipal = actionLog.getCurrentStock();
            if (currentStockPrincipal == 0L) {
                averagePurchasePrice = null;
                previousStockPrincipal = 0L;
                continue;
            }

            if (previousStockPrincipal == 0L || averagePurchasePrice == null) {
                averagePurchasePrice = getScenarioPrice(scenario, actionLog.getGameTick());
            } else if (currentStockPrincipal > previousStockPrincipal) {
                averagePurchasePrice = calculateWeightedAveragePurchasePrice(
                        scenario,
                        actionLog,
                        previousStockPrincipal,
                        currentStockPrincipal,
                        averagePurchasePrice
                );
            }
            previousStockPrincipal = currentStockPrincipal;
        }

        return averagePurchasePrice;
    }

    private BigDecimal calculateWeightedAveragePurchasePrice(
            ScenarioDto scenario,
            ActionLogDto actionLog,
            long previousStockPrincipal,
            long currentStockPrincipal,
            BigDecimal previousAveragePurchasePrice) {
        BigDecimal previousQuantity = BigDecimal.valueOf(previousStockPrincipal)
                .divide(previousAveragePurchasePrice, QUANTITY_SCALE, RoundingMode.HALF_UP);
        BigDecimal addedPrincipal = BigDecimal.valueOf(
                currentStockPrincipal - previousStockPrincipal
        );
        BigDecimal purchasePrice = getScenarioPrice(scenario, actionLog.getGameTick());
        BigDecimal addedQuantity = addedPrincipal.divide(
                purchasePrice,
                QUANTITY_SCALE,
                RoundingMode.HALF_UP
        );
        BigDecimal totalQuantity = previousQuantity.add(addedQuantity);

        return BigDecimal.valueOf(currentStockPrincipal)
                .divide(totalQuantity, PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal getScenarioPrice(ScenarioDto scenario, int gameTick) {
        if (scenario == null || scenario.getTicks() == null) {
            throw new IllegalArgumentException("게임 시나리오 tick 정보는 필수입니다.");
        }

        return scenario.getTicks()
                .stream()
                .filter(scenarioTick -> scenarioTick.getTick() == gameTick)
                .map(ScenarioTickDto::getPrice)
                .map(BigDecimal::valueOf)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "게임 시나리오 tick을 찾을 수 없습니다: " + gameTick
                ));
    }
}
