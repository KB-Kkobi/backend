package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.Consumer;

public class GameBehaviorSimulationAnalyzer {

    private static final long GAME_SEED_MONEY = 10_000_000L;
    private static final int TOTAL_RATIO = 100;
    private static final int STATISTICS_SCALE = 4;
    private static final int DISTRIBUTION_SCALE = 2;
    private static final MathContext STATISTICS_CONTEXT =
            new MathContext(20, RoundingMode.HALF_UP);

    private final GameBehaviorSimulator gameBehaviorSimulator =
            new GameBehaviorSimulator();

    public GameBehaviorSimulationAnalysis analyzeGameSimulations(
            ScenarioDto scenario,
            int simulationCount,
            long randomSeed) {
        return analyzeGameSimulations(
                scenario,
                simulationCount,
                randomSeed,
                simulationResult -> {
                }
        );
    }

    public GameBehaviorSimulationAnalysis analyzeGameSimulations(
            ScenarioDto scenario,
            int simulationCount,
            long randomSeed,
            Consumer<GameBehaviorSimulationResult> simulationResultConsumer) {
        validateAnalysisInput(scenario, simulationCount);
        Objects.requireNonNull(
                simulationResultConsumer,
                "시뮬레이션 결과 처리 함수는 필수입니다."
        );
        long initialStockPrice = getInitialStockPrice(scenario);
        SplittableRandom random = new SplittableRandom(randomSeed);
        EnumMap<PersonaType, MutablePersonaSummary> personaSummaryByType =
                createPersonaSummaryByType();
        MutableScoreSummary overallRtScoreSummary = new MutableScoreSummary();
        MutableScoreSummary overallLhScoreSummary = new MutableScoreSummary();
        MutableScoreSummary overallRpScoreSummary = new MutableScoreSummary();

        for (int index = 0; index < simulationCount; index++) {
            SplittableRandom userRandom = random.split();
            GameBehaviorSimulationResult simulationResult = gameBehaviorSimulator.simulateGame(
                    index + 1L,
                    scenario,
                    createInitialPortfolio(initialStockPrice, userRandom),
                    userRandom.nextLong()
            );
            simulationResultConsumer.accept(simulationResult);
            personaSummaryByType.get(simulationResult.getPersonaType())
                    .addSimulationResult(simulationResult);
            overallRtScoreSummary.addScore(simulationResult.getFinalRtScore());
            overallLhScoreSummary.addScore(simulationResult.getFinalLhScore());
            overallRpScoreSummary.addScore(simulationResult.getFinalRpScore());
        }

        return new GameBehaviorSimulationAnalysis(
                simulationCount,
                createPersonaSummaries(personaSummaryByType, simulationCount),
                overallRtScoreSummary.createScoreSummary(),
                overallLhScoreSummary.createScoreSummary(),
                overallRpScoreSummary.createScoreSummary()
        );
    }

    private SimulatedGamePortfolio createInitialPortfolio(
            long initialStockPrice,
            SplittableRandom random) {
        int[] assetRatios = createAssetRatios(random);
        long targetCash = calculateAllocationAmount(assetRatios[0]);
        long targetStockPrincipal = calculateAllocationAmount(assetRatios[1]);
        long currentDeposit = calculateAllocationAmount(assetRatios[2]);
        int stockQuantity = (int) Math.min(
                targetStockPrincipal / initialStockPrice,
                Integer.MAX_VALUE
        );
        long currentStockPrincipal = Math.multiplyExact(initialStockPrice, stockQuantity);
        long currentCash = Math.addExact(
                targetCash,
                targetStockPrincipal - currentStockPrincipal
        );

        return new SimulatedGamePortfolio(
                currentCash,
                currentStockPrincipal,
                currentDeposit,
                stockQuantity
        );
    }

    private int[] createAssetRatios(SplittableRandom random) {
        int firstBoundary = random.nextInt(TOTAL_RATIO + 1);
        int secondBoundary = random.nextInt(TOTAL_RATIO + 1);
        int lowerBoundary = Math.min(firstBoundary, secondBoundary);
        int upperBoundary = Math.max(firstBoundary, secondBoundary);
        int[] assetRatios = {
                lowerBoundary,
                upperBoundary - lowerBoundary,
                TOTAL_RATIO - upperBoundary
        };

        for (int index = assetRatios.length - 1; index > 0; index--) {
            int exchangeIndex = random.nextInt(index + 1);
            int selectedRatio = assetRatios[index];
            assetRatios[index] = assetRatios[exchangeIndex];
            assetRatios[exchangeIndex] = selectedRatio;
        }
        return assetRatios;
    }

    private long calculateAllocationAmount(int assetRatio) {
        return GAME_SEED_MONEY * assetRatio / TOTAL_RATIO;
    }

    private EnumMap<PersonaType, MutablePersonaSummary> createPersonaSummaryByType() {
        EnumMap<PersonaType, MutablePersonaSummary> summaries =
                new EnumMap<>(PersonaType.class);
        for (PersonaType personaType : PersonaType.values()) {
            summaries.put(personaType, new MutablePersonaSummary(personaType));
        }
        return summaries;
    }

    private Map<PersonaType, GameBehaviorSimulationAnalysis.PersonaSummary> createPersonaSummaries(
            Map<PersonaType, MutablePersonaSummary> mutableSummaries,
            int totalSimulationCount) {
        EnumMap<PersonaType, GameBehaviorSimulationAnalysis.PersonaSummary> summaries =
                new EnumMap<>(PersonaType.class);
        mutableSummaries.forEach((personaType, mutableSummary) -> summaries.put(
                personaType,
                mutableSummary.createPersonaSummary(totalSimulationCount)
        ));
        return summaries;
    }

    private long getInitialStockPrice(ScenarioDto scenario) {
        return scenario.getTicks()
                .stream()
                .filter(scenarioTick -> scenarioTick.getTick() == 0)
                .mapToLong(ScenarioTickDto::getPrice)
                .filter(price -> price > 0)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "초기 주식 가격을 찾을 수 없습니다."
                ));
    }

    private void validateAnalysisInput(
            ScenarioDto scenario,
            int simulationCount) {
        if (scenario == null || scenario.getTicks() == null) {
            throw new IllegalArgumentException("게임 시나리오 Tick 정보는 필수입니다.");
        }
        if (simulationCount <= 0) {
            throw new IllegalArgumentException("시뮬레이션 수는 0보다 커야 합니다.");
        }
    }

    private static class MutablePersonaSummary {

        private final PersonaType personaType;
        private final MutableScoreSummary rtScoreSummary = new MutableScoreSummary();
        private final MutableScoreSummary lhScoreSummary = new MutableScoreSummary();
        private final MutableScoreSummary rpScoreSummary = new MutableScoreSummary();
        private int simulationCount;

        private MutablePersonaSummary(PersonaType personaType) {
            this.personaType = personaType;
        }

        private void addSimulationResult(GameBehaviorSimulationResult simulationResult) {
            simulationCount++;
            rtScoreSummary.addScore(simulationResult.getFinalRtScore());
            lhScoreSummary.addScore(simulationResult.getFinalLhScore());
            rpScoreSummary.addScore(simulationResult.getFinalRpScore());
        }

        private GameBehaviorSimulationAnalysis.PersonaSummary createPersonaSummary(
                int totalSimulationCount) {
            BigDecimal distributionRate = BigDecimal.valueOf(simulationCount)
                    .multiply(BigDecimal.valueOf(TOTAL_RATIO))
                    .divide(
                            BigDecimal.valueOf(totalSimulationCount),
                            DISTRIBUTION_SCALE,
                            RoundingMode.HALF_UP
                    );
            return new GameBehaviorSimulationAnalysis.PersonaSummary(
                    personaType,
                    simulationCount,
                    distributionRate,
                    rtScoreSummary.createScoreSummary(),
                    lhScoreSummary.createScoreSummary(),
                    rpScoreSummary.createScoreSummary()
            );
        }
    }

    private static class MutableScoreSummary {

        private BigDecimal scoreSum = BigDecimal.ZERO;
        private BigDecimal squaredScoreSum = BigDecimal.ZERO;
        private BigDecimal minimumScore;
        private BigDecimal maximumScore;
        private int scoreCount;

        private void addScore(BigDecimal score) {
            if (score == null) {
                throw new IllegalArgumentException("성향 점수는 필수입니다.");
            }
            scoreSum = scoreSum.add(score);
            squaredScoreSum = squaredScoreSum.add(score.multiply(score));
            minimumScore = minimumScore == null ? score : minimumScore.min(score);
            maximumScore = maximumScore == null ? score : maximumScore.max(score);
            scoreCount++;
        }

        private GameBehaviorSimulationAnalysis.ScoreSummary createScoreSummary() {
            if (scoreCount == 0) {
                BigDecimal zeroScore = BigDecimal.ZERO.setScale(STATISTICS_SCALE);
                return new GameBehaviorSimulationAnalysis.ScoreSummary(
                        zeroScore,
                        zeroScore,
                        zeroScore,
                        zeroScore
                );
            }

            BigDecimal scoreCountValue = BigDecimal.valueOf(scoreCount);
            BigDecimal average = scoreSum.divide(scoreCountValue, STATISTICS_CONTEXT);
            BigDecimal variance = squaredScoreSum
                    .divide(scoreCountValue, STATISTICS_CONTEXT)
                    .subtract(average.multiply(average, STATISTICS_CONTEXT), STATISTICS_CONTEXT)
                    .max(BigDecimal.ZERO);
            BigDecimal standardDeviation = variance.sqrt(STATISTICS_CONTEXT);

            return new GameBehaviorSimulationAnalysis.ScoreSummary(
                    average.setScale(STATISTICS_SCALE, RoundingMode.HALF_UP),
                    standardDeviation.setScale(STATISTICS_SCALE, RoundingMode.HALF_UP),
                    minimumScore.setScale(STATISTICS_SCALE, RoundingMode.HALF_UP),
                    maximumScore.setScale(STATISTICS_SCALE, RoundingMode.HALF_UP)
            );
        }
    }
}
