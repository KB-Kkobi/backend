package org.kkobi.assessment.simulation;

import java.util.SplittableRandom;

public class PersonaInitialPortfolioFactory {

    private static final long SEED_MONEY = 10_000_000L;

    public SimulatedGamePortfolio create(
            PersonaBehaviorProfile profile,
            long initialStockPrice,
            SplittableRandom random) {
        if (profile == null || random == null) {
            throw new IllegalArgumentException("프로필과 난수 생성기는 필수입니다.");
        }
        if (initialStockPrice <= 0) {
            throw new IllegalArgumentException("초기 주가는 0보다 커야 합니다.");
        }

        int stockRatio = nextRatio(
                random,
                profile.minimumStockRatio(),
                profile.maximumStockRatio()
        );
        int maximumDepositRatio = Math.min(
                profile.maximumDepositRatio(),
                100 - stockRatio
        );
        int minimumDepositRatio = Math.min(
                profile.minimumDepositRatio(),
                maximumDepositRatio
        );
        int depositRatio = nextRatio(random, minimumDepositRatio, maximumDepositRatio);
        int cashRatio = 100 - stockRatio - depositRatio;

        long targetStockAmount = SEED_MONEY * stockRatio / 100;
        long depositAmount = SEED_MONEY * depositRatio / 100;
        int stockQuantity = (int) (targetStockAmount / initialStockPrice);
        long stockAmount = initialStockPrice * stockQuantity;
        long cashAmount = SEED_MONEY - stockAmount - depositAmount;
        return new SimulatedGamePortfolio(
                cashAmount,
                stockAmount,
                depositAmount,
                stockQuantity
        );
    }

    private int nextRatio(SplittableRandom random, int minimum, int maximum) {
        return minimum == maximum ? minimum : random.nextInt(minimum, maximum + 1);
    }
}
