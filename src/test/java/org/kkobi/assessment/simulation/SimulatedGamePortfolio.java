package org.kkobi.assessment.simulation;

import lombok.Getter;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class SimulatedGamePortfolio {

    private static final int TOTAL_GAME_TICKS = 52;
    private static final int PRICE_SCALE = 8;
    private static final int RETURN_RATE_SCALE = 2;
    private static final BigDecimal PERCENTAGE = BigDecimal.valueOf(100);

    private long currentCash;
    private long currentStockPrincipal;
    private long currentDeposit;
    private int currentStockQuantity;
    private BigDecimal averagePurchasePrice;
    private boolean depositCancelled;
    private boolean depositMatured;

    public SimulatedGamePortfolio(
            long currentCash,
            long currentStockPrincipal,
            long currentDeposit,
            int currentStockQuantity) {
        this.currentCash = validateAmount(currentCash, "현재 현금");
        this.currentStockPrincipal = validateAmount(currentStockPrincipal, "현재 주식 매입 원금");
        this.currentDeposit = validateAmount(currentDeposit, "현재 예금");
        this.currentStockQuantity = validateStockQuantity(currentStockPrincipal, currentStockQuantity);
        this.averagePurchasePrice = calculateAveragePurchasePrice();
    }

    public SimulatedGameAction buyStock(
            int gameTick,
            int quantity,
            long executionPrice) {
        validateGameTick(gameTick);
        validatePositiveQuantity(quantity);
        validatePositivePrice(executionPrice);

        long orderAmount = multiplyAmount(executionPrice, quantity);
        if (orderAmount > currentCash) {
            throw new IllegalStateException("보유 현금보다 많은 주식을 매수할 수 없습니다.");
        }

        BigDecimal positionReturnRate = calculateReturnRate(executionPrice);
        currentCash -= orderAmount;
        currentStockPrincipal = addAmount(currentStockPrincipal, orderAmount);
        currentStockQuantity = Math.addExact(currentStockQuantity, quantity);
        averagePurchasePrice = calculateAveragePurchasePrice();

        return createSecurityAction(
                gameTick,
                BehaviorActionType.BUY,
                orderAmount,
                quantity,
                executionPrice,
                positionReturnRate,
                null
        );
    }

    public SimulatedGameAction sellStock(
            int gameTick,
            int quantity,
            long executionPrice) {
        validateGameTick(gameTick);
        validatePositiveQuantity(quantity);
        validatePositivePrice(executionPrice);
        if (quantity > currentStockQuantity) {
            throw new IllegalStateException("보유 수량보다 많은 주식을 매도할 수 없습니다.");
        }

        long orderAmount = multiplyAmount(executionPrice, quantity);
        BigDecimal realizedReturnRate = calculateReturnRate(executionPrice);
        long soldPrincipal = calculateSoldPrincipal(quantity);

        currentCash = addAmount(currentCash, orderAmount);
        currentStockPrincipal -= soldPrincipal;
        currentStockQuantity -= quantity;
        averagePurchasePrice = calculateAveragePurchasePrice();

        return createSecurityAction(
                gameTick,
                BehaviorActionType.SELL,
                orderAmount,
                quantity,
                executionPrice,
                null,
                realizedReturnRate
        );
    }

    public SimulatedGameAction cancelDeposit(int gameTick) {
        validateGameTick(gameTick);
        validateActiveDeposit();

        long cancelledAmount = currentDeposit;
        currentCash = addAmount(currentCash, cancelledAmount);
        currentDeposit = 0L;
        depositCancelled = true;

        return createDepositAction(
                gameTick,
                BehaviorActionType.CANCEL_PRODUCT,
                cancelledAmount
        );
    }

    public SimulatedGameAction matureDeposit(
            int gameTick,
            long interestAmount) {
        if (gameTick != TOTAL_GAME_TICKS) {
            throw new IllegalArgumentException("예금 만기는 게임 종료 Tick에서만 처리할 수 있습니다.");
        }
        validateActiveDeposit();
        validateAmount(interestAmount, "예금 이자");

        long maturityAmount = addAmount(currentDeposit, interestAmount);
        currentCash = addAmount(currentCash, maturityAmount);
        currentDeposit = 0L;
        depositMatured = true;

        return createDepositAction(
                gameTick,
                BehaviorActionType.MATURITY,
                maturityAmount
        );
    }

    public long getCurrentTotalAssetPrincipal() {
        return addAmount(
                addAmount(currentCash, currentStockPrincipal),
                currentDeposit
        );
    }

    private SimulatedGameAction createSecurityAction(
            int gameTick,
            BehaviorActionType actionType,
            long actionAmount,
            int quantity,
            long executionPrice,
            BigDecimal positionReturnRate,
            BigDecimal realizedReturnRate) {
        return new SimulatedGameAction(
                gameTick,
                actionType,
                BehaviorAssetType.SECURITY,
                actionAmount,
                quantity,
                executionPrice,
                positionReturnRate,
                realizedReturnRate,
                currentCash,
                currentStockPrincipal,
                currentDeposit,
                currentStockQuantity
        );
    }

    private SimulatedGameAction createDepositAction(
            int gameTick,
            BehaviorActionType actionType,
            long actionAmount) {
        return new SimulatedGameAction(
                gameTick,
                actionType,
                BehaviorAssetType.PRODUCT,
                actionAmount,
                null,
                null,
                null,
                null,
                currentCash,
                currentStockPrincipal,
                currentDeposit,
                currentStockQuantity
        );
    }

    private BigDecimal calculateAveragePurchasePrice() {
        if (currentStockQuantity == 0) {
            return null;
        }
        return BigDecimal.valueOf(currentStockPrincipal)
                .divide(
                        BigDecimal.valueOf(currentStockQuantity),
                        PRICE_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateReturnRate(long executionPrice) {
        if (averagePurchasePrice == null || averagePurchasePrice.signum() == 0) {
            return null;
        }
        return BigDecimal.valueOf(executionPrice)
                .subtract(averagePurchasePrice)
                .multiply(PERCENTAGE)
                .divide(
                        averagePurchasePrice,
                        RETURN_RATE_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    private long calculateSoldPrincipal(int quantity) {
        if (quantity == currentStockQuantity) {
            return currentStockPrincipal;
        }
        return BigDecimal.valueOf(currentStockPrincipal)
                .multiply(BigDecimal.valueOf(quantity))
                .divide(
                        BigDecimal.valueOf(currentStockQuantity),
                        0,
                        RoundingMode.HALF_UP
                )
                .longValueExact();
    }

    private long validateAmount(long amount, String amountName) {
        if (amount < 0) {
            throw new IllegalArgumentException(amountName + "은 0 이상이어야 합니다.");
        }
        return amount;
    }

    private int validateStockQuantity(long stockPrincipal, int stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("현재 주식 수량은 0 이상이어야 합니다.");
        }
        boolean hasStockPrincipal = stockPrincipal > 0;
        boolean hasStockQuantity = stockQuantity > 0;
        if (hasStockPrincipal != hasStockQuantity) {
            throw new IllegalArgumentException("주식 매입 원금과 보유 수량은 함께 존재해야 합니다.");
        }
        return stockQuantity;
    }

    private void validateGameTick(int gameTick) {
        if (gameTick < 0 || gameTick > TOTAL_GAME_TICKS) {
            throw new IllegalArgumentException("게임 Tick은 0 이상 52 이하여야 합니다.");
        }
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("거래 수량은 0보다 커야 합니다.");
        }
    }

    private void validatePositivePrice(long executionPrice) {
        if (executionPrice <= 0) {
            throw new IllegalArgumentException("거래 가격은 0보다 커야 합니다.");
        }
    }

    private void validateActiveDeposit() {
        if (currentDeposit <= 0) {
            throw new IllegalStateException("보유 중인 예금이 없습니다.");
        }
        if (depositCancelled || depositMatured) {
            throw new IllegalStateException("이미 종료된 예금입니다.");
        }
    }

    private long multiplyAmount(long price, int quantity) {
        try {
            return Math.multiplyExact(price, quantity);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("거래 금액이 허용 범위를 초과했습니다.", exception);
        }
    }

    private long addAmount(long firstAmount, long secondAmount) {
        try {
            return Math.addExact(firstAmount, secondAmount);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("자산 금액이 허용 범위를 초과했습니다.", exception);
        }
    }
}
