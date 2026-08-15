package org.kkobi.assessment.simulation;

import org.kkobi.assessment.calculator.AssetRatioCalculator;
import org.kkobi.assessment.calculator.BehaviorContextFactory;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.GameScoreCalculator;
import org.kkobi.assessment.calculator.MarketStateCalculator;
import org.kkobi.assessment.calculator.PersonaClassifier;
import org.kkobi.assessment.calculator.SecurityPriceRateCalculator;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.domain.RuleResult;
import org.kkobi.assessment.domain.ScoreDelta;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.MarketState;
import org.kkobi.game.calculator.GamePriceRateCalculator;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameBehaviorSimulator {

    private static final Long GAME_SECURITY_ID = 1L;
    private static final BigDecimal CRASH_HOLDING_MINIMUM_RATIO = BigDecimal.valueOf(50);
    private static final ScoreDelta CRASH_HOLDING_SCORE =
            ScoreDelta.createScoreDelta(5, 0, 0);
    private static final BigDecimal NORMAL_BUY_MINIMUM_RATIO = BigDecimal.valueOf(10);
    private static final BigDecimal NORMAL_BUY_MAXIMUM_RATIO = BigDecimal.valueOf(30);
    private static final ScoreDelta NORMAL_PLANNED_BUY_SCORE =
            ScoreDelta.createScoreDelta(0, -5, 5);
    private static final BigDecimal CASH_BUFFER_MINIMUM_RATIO = BigDecimal.valueOf(25);
    private static final BigDecimal CASH_BUFFER_MAXIMUM_RATIO = BigDecimal.valueOf(50);
    private static final int CASH_BUFFER_MAINTENANCE_TICKS = 3;
    private static final ScoreDelta CASH_BUFFER_MAINTENANCE_SCORE =
            ScoreDelta.createScoreDelta(0, 5, 0);

    private final NeutralGameBehaviorGenerator behaviorGenerator =
            new NeutralGameBehaviorGenerator();
    private final AssetRatioCalculator assetRatioCalculator =
            new AssetRatioCalculator();
    private final MarketStateCalculator marketStateCalculator =
            new MarketStateCalculator();
    private final BehaviorContextFactory behaviorContextFactory =
            new BehaviorContextFactory(
                    assetRatioCalculator,
                    marketStateCalculator
            );
    private final BehaviorRuleEngine behaviorRuleEngine =
            new BehaviorRuleEngine();
    private final GameScoreCalculator gameScoreCalculator =
            new GameScoreCalculator();
    private final PersonaClassifier personaClassifier =
            new PersonaClassifier();
    private final GamePriceRateCalculator gamePriceRateCalculator =
            new GamePriceRateCalculator(new SecurityPriceRateCalculator());

    public GameBehaviorSimulationResult simulateGame(
            long simulationUserId,
            ScenarioDto scenario,
            SimulatedGamePortfolio initialPortfolio,
            long randomSeed) {
        return simulateGame(
                simulationUserId,
                scenario,
                initialPortfolio,
                randomSeed,
                null,
                ConsecutiveActionMultiplierCondition.ENABLED,
                SameTickRuleApplicationCondition.REPEATED,
                RuleAccumulationCondition.UNLIMITED
        );
    }

    public GameBehaviorSimulationResult simulateGame(
            long simulationUserId,
            ScenarioDto scenario,
            SimulatedGamePortfolio initialPortfolio,
            long randomSeed,
            GameBehaviorFrequencyCondition frequencyCondition) {
        return simulateGame(
                simulationUserId,
                scenario,
                initialPortfolio,
                randomSeed,
                frequencyCondition,
                ConsecutiveActionMultiplierCondition.ENABLED,
                SameTickRuleApplicationCondition.REPEATED,
                RuleAccumulationCondition.UNLIMITED
        );
    }

    public GameBehaviorSimulationResult simulateGame(
            long simulationUserId,
            ScenarioDto scenario,
            SimulatedGamePortfolio initialPortfolio,
            long randomSeed,
            GameBehaviorFrequencyCondition frequencyCondition,
            ConsecutiveActionMultiplierCondition multiplierCondition) {
        return simulateGame(
                simulationUserId,
                scenario,
                initialPortfolio,
                randomSeed,
                frequencyCondition,
                multiplierCondition,
                SameTickRuleApplicationCondition.REPEATED,
                RuleAccumulationCondition.UNLIMITED
        );
    }

    public GameBehaviorSimulationResult simulateGame(
            long simulationUserId,
            ScenarioDto scenario,
            SimulatedGamePortfolio initialPortfolio,
            long randomSeed,
            GameBehaviorFrequencyCondition frequencyCondition,
            ConsecutiveActionMultiplierCondition multiplierCondition,
            SameTickRuleApplicationCondition sameTickRuleCondition) {
        return simulateGame(
                simulationUserId,
                scenario,
                initialPortfolio,
                randomSeed,
                frequencyCondition,
                multiplierCondition,
                sameTickRuleCondition,
                RuleAccumulationCondition.UNLIMITED
        );
    }

    public GameBehaviorSimulationResult simulateGame(
            long simulationUserId,
            ScenarioDto scenario,
            SimulatedGamePortfolio initialPortfolio,
            long randomSeed,
            GameBehaviorFrequencyCondition frequencyCondition,
            ConsecutiveActionMultiplierCondition multiplierCondition,
            SameTickRuleApplicationCondition sameTickRuleCondition,
            RuleAccumulationCondition ruleAccumulationCondition) {
        return simulateGame(
                simulationUserId,
                scenario,
                initialPortfolio,
                randomSeed,
                frequencyCondition,
                multiplierCondition,
                sameTickRuleCondition,
                ruleAccumulationCondition,
                LossAveragingRtWeightCondition.RT_15
        );
    }

    public GameBehaviorSimulationResult simulateGame(
            long simulationUserId,
            ScenarioDto scenario,
            SimulatedGamePortfolio initialPortfolio,
            long randomSeed,
            GameBehaviorFrequencyCondition frequencyCondition,
            ConsecutiveActionMultiplierCondition multiplierCondition,
            SameTickRuleApplicationCondition sameTickRuleCondition,
            RuleAccumulationCondition ruleAccumulationCondition,
            LossAveragingRtWeightCondition lossAveragingRtWeightCondition) {
        return simulateGame(
                simulationUserId,
                scenario,
                initialPortfolio,
                randomSeed,
                frequencyCondition,
                multiplierCondition,
                sameTickRuleCondition,
                ruleAccumulationCondition,
                lossAveragingRtWeightCondition,
                GameRuleEvaluationCondition.BASELINE
        );
    }

    public GameBehaviorSimulationResult simulateGame(
            long simulationUserId,
            ScenarioDto scenario,
            SimulatedGamePortfolio initialPortfolio,
            long randomSeed,
            GameBehaviorFrequencyCondition frequencyCondition,
            ConsecutiveActionMultiplierCondition multiplierCondition,
            SameTickRuleApplicationCondition sameTickRuleCondition,
            RuleAccumulationCondition ruleAccumulationCondition,
            LossAveragingRtWeightCondition lossAveragingRtWeightCondition,
            GameRuleEvaluationCondition ruleEvaluationCondition) {
        return simulateGame(
                simulationUserId,
                scenario,
                initialPortfolio,
                randomSeed,
                frequencyCondition,
                multiplierCondition,
                sameTickRuleCondition,
                ruleAccumulationCondition,
                lossAveragingRtWeightCondition,
                ruleEvaluationCondition,
                TradeQuantityGenerationCondition.CURRENT_RANDOM_BUY_PERCENTAGE
        );
    }

    public GameBehaviorSimulationResult simulateGame(
            long simulationUserId,
            ScenarioDto scenario,
            SimulatedGamePortfolio initialPortfolio,
            long randomSeed,
            GameBehaviorFrequencyCondition frequencyCondition,
            ConsecutiveActionMultiplierCondition multiplierCondition,
            SameTickRuleApplicationCondition sameTickRuleCondition,
            RuleAccumulationCondition ruleAccumulationCondition,
            LossAveragingRtWeightCondition lossAveragingRtWeightCondition,
            GameRuleEvaluationCondition ruleEvaluationCondition,
            TradeQuantityGenerationCondition tradeQuantityCondition) {
        validateSimulationInput(simulationUserId, scenario, initialPortfolio);
        if (multiplierCondition == null) {
            throw new IllegalArgumentException("연속 행동 배율 조건은 필수입니다.");
        }
        if (sameTickRuleCondition == null) {
            throw new IllegalArgumentException("동일 Tick 규칙 적용 조건은 필수입니다.");
        }
        if (ruleAccumulationCondition == null) {
            throw new IllegalArgumentException("규칙 누적 조건은 필수입니다.");
        }
        if (lossAveragingRtWeightCondition == null) {
            throw new IllegalArgumentException("물타기 RT 가중치 조건은 필수입니다.");
        }
        if (ruleEvaluationCondition == null) {
            throw new IllegalArgumentException("게임 규칙 평가 조건은 필수입니다.");
        }
        if (tradeQuantityCondition == null) {
            throw new IllegalArgumentException("거래 수량 생성 조건은 필수입니다.");
        }

        long initialCash = initialPortfolio.getCurrentCash();
        long initialStockPrincipal = initialPortfolio.getCurrentStockPrincipal();
        long initialDeposit = initialPortfolio.getCurrentDeposit();
        int initialStockQuantity = initialPortfolio.getCurrentStockQuantity();
        SimulatedGamePortfolio simulationPortfolio = new SimulatedGamePortfolio(
                initialCash,
                initialStockPrincipal,
                initialDeposit,
                initialStockQuantity
        );
        GameBehaviorGenerationResult generationResult = behaviorGenerator.generateGameBehavior(
                scenario,
                simulationPortfolio,
                randomSeed,
                frequencyCondition,
                tradeQuantityCondition
        );

        List<BehaviorEvent> behaviorEvents = new ArrayList<>();
        List<BehaviorContext> behaviorContexts = new ArrayList<>();
        List<BehaviorAnalysisResult> analysisResults = new ArrayList<>();
        Map<Integer, Set<BehaviorRuleCode>> appliedRuleCodesByTick = new HashMap<>();
        EnumMap<BehaviorRuleCode, Integer> accumulatedRuleCounts =
                new EnumMap<>(BehaviorRuleCode.class);
        analyzeBehaviorEvent(
                createInitialAllocationEvent(
                        simulationUserId,
                        scenario,
                        initialCash,
                        initialStockPrincipal,
                        initialDeposit,
                        initialStockQuantity
                ),
                behaviorEvents,
                behaviorContexts,
                analysisResults,
                multiplierCondition,
                sameTickRuleCondition,
                appliedRuleCodesByTick,
                ruleAccumulationCondition,
                accumulatedRuleCounts,
                lossAveragingRtWeightCondition,
                ruleEvaluationCondition
        );

        long actionSequence = 1L;
        for (SimulatedGameAction action : generationResult.getActions()) {
            analyzeBehaviorEvent(
                    createBehaviorEvent(
                            simulationUserId,
                            actionSequence++,
                            scenario,
                            action
                    ),
                    behaviorEvents,
                    behaviorContexts,
                    analysisResults,
                    multiplierCondition,
                    sameTickRuleCondition,
                    appliedRuleCodesByTick,
                    ruleAccumulationCondition,
                    accumulatedRuleCounts,
                    lossAveragingRtWeightCondition,
                    ruleEvaluationCondition
            );
        }

        analysisResults = ruleEvaluationCondition.adjustDepositDecisionResults(
                behaviorContexts,
                analysisResults
        );

        int crashHoldingEpisodeCount = ruleEvaluationCondition.appliesCrashHoldingRule()
                ? calculateCrashHoldingEpisodeCount(
                scenario,
                initialStockQuantity,
                generationResult.getActions()
        )
                : 0;
        int normalPlannedBuyCount = ruleEvaluationCondition.appliesNormalPlannedBuyRule()
                ? calculateNormalPlannedBuyCount(behaviorContexts, analysisResults)
                : 0;
        int cashBufferMaintenanceCount =
                ruleEvaluationCondition.appliesCashBufferMaintenanceRule()
                        ? ruleEvaluationCondition.appliesCashBufferMaintenancePerEpisodeRule()
                                ? calculateCashBufferMaintenanceEpisodeCount(
                                scenario,
                                initialCash,
                                initialStockPrincipal,
                                initialDeposit,
                                generationResult.getActions()
                        )
                                : calculateCashBufferMaintenanceCount(
                                scenario,
                                initialCash,
                                initialStockPrincipal,
                                initialDeposit,
                                generationResult.getActions()
                        )
                        : 0;
        List<ScoreDelta> scoreDeltas = new ArrayList<>(analysisResults.stream()
                .map(BehaviorAnalysisResult::getTotalScoreDelta)
                .toList());
        for (int count = 0; count < crashHoldingEpisodeCount; count++) {
            scoreDeltas.add(CRASH_HOLDING_SCORE);
        }
        for (int count = 0; count < normalPlannedBuyCount; count++) {
            scoreDeltas.add(NORMAL_PLANNED_BUY_SCORE);
        }
        for (int count = 0; count < cashBufferMaintenanceCount; count++) {
            scoreDeltas.add(CASH_BUFFER_MAINTENANCE_SCORE);
        }
        AssessmentScore assessmentScore = gameScoreCalculator.calculateGameScore(scoreDeltas);
        Map<BehaviorRuleCode, Integer> ruleApplicationCounts = calculateRuleApplicationCounts(
                analysisResults
        );
        Map<BehaviorRuleCode, ScoreDelta> ruleScoreContributions =
                calculateRuleScoreContributions(analysisResults);

        return createSimulationResult(
                simulationUserId,
                initialCash,
                initialStockPrincipal,
                initialDeposit,
                generationResult,
                behaviorContexts,
                crashHoldingEpisodeCount,
                normalPlannedBuyCount,
                cashBufferMaintenanceCount,
                assessmentScore,
                ruleApplicationCounts,
                ruleScoreContributions
        );
    }

    private void analyzeBehaviorEvent(
            BehaviorEvent behaviorEvent,
            List<BehaviorEvent> previousEvents,
            List<BehaviorContext> behaviorContexts,
            List<BehaviorAnalysisResult> analysisResults,
            ConsecutiveActionMultiplierCondition multiplierCondition,
            SameTickRuleApplicationCondition sameTickRuleCondition,
            Map<Integer, Set<BehaviorRuleCode>> appliedRuleCodesByTick,
            RuleAccumulationCondition ruleAccumulationCondition,
            Map<BehaviorRuleCode, Integer> accumulatedRuleCounts,
            LossAveragingRtWeightCondition lossAveragingRtWeightCondition,
            GameRuleEvaluationCondition ruleEvaluationCondition) {
        BehaviorContext behaviorContext = behaviorContextFactory.createBehaviorContext(
                behaviorEvent,
                previousEvents
        );
        BehaviorAnalysisResult analysisResult = behaviorRuleEngine
                .calculateGameBehaviorAnalysis(behaviorContext);
        analysisResult = ruleEvaluationCondition.excludeSmallTradeScores(
                behaviorContext,
                analysisResult
        );
        if (multiplierCondition == ConsecutiveActionMultiplierCondition.DISABLED) {
            analysisResult = removeConsecutiveActionMultiplier(
                    behaviorContext,
                    analysisResult
            );
        }
        if (sameTickRuleCondition == SameTickRuleApplicationCondition.ONCE_PER_TICK) {
            analysisResult = removeSameTickDuplicateRules(
                    behaviorContext,
                    analysisResult,
                    appliedRuleCodesByTick
            );
        }
        analysisResult = applyLossAveragingRtWeight(
                analysisResult,
                lossAveragingRtWeightCondition
        );
        analysisResult = ruleEvaluationCondition.adjustAnalysisResult(
                behaviorContext,
                analysisResult
        );
        analysisResult = applyRuleAccumulationCondition(
                analysisResult,
                ruleAccumulationCondition,
                accumulatedRuleCounts
        );

        behaviorContexts.add(behaviorContext);
        analysisResults.add(analysisResult);
        previousEvents.add(behaviorEvent);
    }

    private BehaviorAnalysisResult applyLossAveragingRtWeight(
            BehaviorAnalysisResult analysisResult,
            LossAveragingRtWeightCondition lossAveragingRtWeightCondition) {
        List<RuleResult> adjustedRules = analysisResult.getAppliedRules().stream()
                .map(lossAveragingRtWeightCondition::adjustRuleResult)
                .toList();
        return new BehaviorAnalysisResult(adjustedRules);
    }

    private BehaviorAnalysisResult applyRuleAccumulationCondition(
            BehaviorAnalysisResult analysisResult,
            RuleAccumulationCondition ruleAccumulationCondition,
            Map<BehaviorRuleCode, Integer> accumulatedRuleCounts) {
        if (ruleAccumulationCondition == RuleAccumulationCondition.UNLIMITED
                || analysisResult.getAppliedRules().isEmpty()) {
            analysisResult.getAppliedRules().forEach(ruleResult -> accumulatedRuleCounts.merge(
                    ruleResult.getRuleCode(),
                    1,
                    Integer::sum
            ));
            return analysisResult;
        }

        List<RuleResult> adjustedRules = new ArrayList<>();
        for (RuleResult ruleResult : analysisResult.getAppliedRules()) {
            int previousApplicationCount = accumulatedRuleCounts.getOrDefault(
                    ruleResult.getRuleCode(),
                    0
            );
            accumulatedRuleCounts.put(
                    ruleResult.getRuleCode(),
                    previousApplicationCount + 1
            );
            int applicationLimit = ruleAccumulationCondition.getApplicationLimit(
                    ruleResult.getRuleCode()
            );
            if (previousApplicationCount < applicationLimit) {
                adjustedRules.add(ruleResult);
            } else if (ruleAccumulationCondition.isHalfAttenuation()) {
                adjustedRules.add(ruleResult.multiplyScoreDelta(BigDecimal.valueOf(0.5)));
            }
        }
        return new BehaviorAnalysisResult(adjustedRules);
    }

    private BehaviorAnalysisResult removeSameTickDuplicateRules(
            BehaviorContext behaviorContext,
            BehaviorAnalysisResult analysisResult,
            Map<Integer, Set<BehaviorRuleCode>> appliedRuleCodesByTick) {
        Integer gameTick = behaviorContext.getCurrentEvent() == null
                ? null
                : behaviorContext.getCurrentEvent().getGameTick();
        if (gameTick == null || analysisResult.getAppliedRules().isEmpty()) {
            return analysisResult;
        }

        Set<BehaviorRuleCode> appliedRuleCodes = appliedRuleCodesByTick.computeIfAbsent(
                gameTick,
                ignored -> new HashSet<>()
        );
        List<RuleResult> uniqueRules = analysisResult.getAppliedRules().stream()
                .filter(ruleResult -> appliedRuleCodes.add(ruleResult.getRuleCode()))
                .toList();
        return new BehaviorAnalysisResult(uniqueRules);
    }

    private BehaviorAnalysisResult removeConsecutiveActionMultiplier(
            BehaviorContext behaviorContext,
            BehaviorAnalysisResult analysisResult) {
        if (behaviorContext.getCurrentEvent() == null
                || behaviorContext.getConsecutiveActionCount() == null
                || behaviorContext.getConsecutiveActionCount() <= 1
                || behaviorContext.getCurrentEvent().getActionType()
                == BehaviorActionType.INITIAL_ALLOCATION) {
            return analysisResult;
        }

        BigDecimal multiplier = behaviorContext.getConsecutiveActionCount() == 2
                ? BigDecimal.valueOf(1.2)
                : BigDecimal.valueOf(1.5);
        List<RuleResult> normalizedRules = analysisResult.getAppliedRules().stream()
                .map(ruleResult -> new RuleResult(
                        ruleResult.getRuleCode(),
                        divideScoreDelta(ruleResult.getScoreDelta(), multiplier),
                        ruleResult.getReason()
                ))
                .toList();
        return new BehaviorAnalysisResult(normalizedRules);
    }

    private ScoreDelta divideScoreDelta(
            ScoreDelta scoreDelta,
            BigDecimal divisor) {
        return new ScoreDelta(
                divideScore(scoreDelta.getRtDelta(), divisor),
                divideScore(scoreDelta.getLhDelta(), divisor),
                divideScore(scoreDelta.getRpDelta(), divisor)
        );
    }

    private BigDecimal divideScore(BigDecimal score, BigDecimal divisor) {
        return score.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private BehaviorEvent createInitialAllocationEvent(
            long simulationUserId,
            ScenarioDto scenario,
            long initialCash,
            long initialStockPrincipal,
            long initialDeposit,
            int initialStockQuantity) {
        BehaviorEvent behaviorEvent = new BehaviorEvent();
        behaviorEvent.setUserId(simulationUserId);
        behaviorEvent.setGameTick(0);
        behaviorEvent.setActionSequence(0L);
        behaviorEvent.setActionType(BehaviorActionType.INITIAL_ALLOCATION);
        behaviorEvent.setAssetType(BehaviorAssetType.ALL);
        behaviorEvent.setActionAmount(addAmounts(
                addAmounts(initialCash, initialStockPrincipal),
                initialDeposit
        ));
        behaviorEvent.setCurrentCash(initialCash);
        behaviorEvent.setCurrentStockPrincipal(initialStockPrincipal);
        behaviorEvent.setCurrentDeposit(initialDeposit);
        behaviorEvent.setCurrentSecurityQuantity(initialStockQuantity);
        behaviorEvent.setCurrentPriceChangeRate(
                gamePriceRateCalculator.calculateTickPriceChangeRate(scenario, 0)
        );
        behaviorEvent.setTradedAt(getScenarioActionAt(scenario, 0));
        return behaviorEvent;
    }

    private BehaviorEvent createBehaviorEvent(
            long simulationUserId,
            long actionSequence,
            ScenarioDto scenario,
            SimulatedGameAction action) {
        BehaviorEvent behaviorEvent = new BehaviorEvent();
        behaviorEvent.setUserId(simulationUserId);
        behaviorEvent.setGameTick(action.getGameTick());
        behaviorEvent.setActionSequence(actionSequence);
        behaviorEvent.setActionType(action.getActionType());
        behaviorEvent.setAssetType(action.getAssetType());
        if (action.getAssetType() == BehaviorAssetType.SECURITY) {
            behaviorEvent.setSecurityId(GAME_SECURITY_ID);
        }
        behaviorEvent.setQuantity(action.getQuantity());
        behaviorEvent.setActionAmount(action.getActionAmount());
        behaviorEvent.setExecutionPrice(action.getExecutionPrice());
        behaviorEvent.setCurrentCash(action.getCurrentCash());
        behaviorEvent.setCurrentStockPrincipal(action.getCurrentStockPrincipal());
        behaviorEvent.setCurrentDeposit(action.getCurrentDeposit());
        behaviorEvent.setCurrentSecurityQuantity(action.getCurrentStockQuantity());
        behaviorEvent.setCurrentPriceChangeRate(
                gamePriceRateCalculator.calculateTickPriceChangeRate(
                        scenario,
                        action.getGameTick()
                )
        );
        behaviorEvent.setPositionReturnRate(action.getPositionReturnRate());
        behaviorEvent.setRealizedReturnRate(action.getRealizedReturnRate());
        behaviorEvent.setTradedAt(getScenarioActionAt(scenario, action.getGameTick()));
        return behaviorEvent;
    }

    private LocalDateTime getScenarioActionAt(
            ScenarioDto scenario,
            int gameTick) {
        return scenario.getTicks()
                .stream()
                .filter(scenarioTick -> scenarioTick.getTick() == gameTick)
                .map(ScenarioTickDto::getDate)
                .filter(scenarioDate -> scenarioDate != null && !scenarioDate.isBlank())
                .map(LocalDate::parse)
                .map(LocalDate::atStartOfDay)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "게임 시나리오 Tick 날짜를 찾을 수 없습니다: " + gameTick
                ));
    }

    private Map<BehaviorRuleCode, Integer> calculateRuleApplicationCounts(
            List<BehaviorAnalysisResult> analysisResults) {
        EnumMap<BehaviorRuleCode, Integer> ruleApplicationCounts =
                new EnumMap<>(BehaviorRuleCode.class);
        analysisResults.stream()
                .map(BehaviorAnalysisResult::getAppliedRules)
                .flatMap(List::stream)
                .map(RuleResult::getRuleCode)
                .forEach(ruleCode -> ruleApplicationCounts.merge(ruleCode, 1, Integer::sum));
        return ruleApplicationCounts;
    }

    private Map<BehaviorRuleCode, ScoreDelta> calculateRuleScoreContributions(
            List<BehaviorAnalysisResult> analysisResults) {
        EnumMap<BehaviorRuleCode, ScoreDelta> ruleScoreContributions =
                new EnumMap<>(BehaviorRuleCode.class);
        analysisResults.stream()
                .map(BehaviorAnalysisResult::getAppliedRules)
                .flatMap(List::stream)
                .forEach(ruleResult -> ruleScoreContributions.merge(
                        ruleResult.getRuleCode(),
                        ruleResult.getScoreDelta(),
                        ScoreDelta::addScoreDelta
                ));
        return ruleScoreContributions;
    }

    private GameBehaviorSimulationResult createSimulationResult(
            long simulationUserId,
            long initialCash,
            long initialStockPrincipal,
            long initialDeposit,
            GameBehaviorGenerationResult generationResult,
            List<BehaviorContext> behaviorContexts,
            int crashHoldingEpisodeCount,
            int normalPlannedBuyCount,
            int cashBufferMaintenanceCount,
            AssessmentScore assessmentScore,
            Map<BehaviorRuleCode, Integer> ruleApplicationCounts,
            Map<BehaviorRuleCode, ScoreDelta> ruleScoreContributions) {
        List<SimulatedGameAction> actions = generationResult.getActions();
        return GameBehaviorSimulationResult.builder()
                .simulationUserId(simulationUserId)
                .initialCashRatio(assetRatioCalculator.calculateCashRatio(
                        initialCash,
                        initialStockPrincipal,
                        initialDeposit
                ))
                .initialStockRatio(assetRatioCalculator.calculateStockRatio(
                        initialCash,
                        initialStockPrincipal,
                        initialDeposit
                ))
                .initialDepositRatio(assetRatioCalculator.calculateDepositRatio(
                        initialCash,
                        initialStockPrincipal,
                        initialDeposit
                ))
                .buyCount(countActions(actions, BehaviorActionType.BUY))
                .sellCount(countActions(actions, BehaviorActionType.SELL))
                .noActionTickCount(generationResult.getNoActionTickCount())
                .totalBuyAmount(calculateTotalActionAmount(actions, BehaviorActionType.BUY))
                .totalSellAmount(calculateTotalActionAmount(actions, BehaviorActionType.SELL))
                .fullSellCount((int) actions.stream()
                        .filter(action -> action.getActionType() == BehaviorActionType.SELL)
                        .filter(action -> action.getCurrentStockQuantity() == 0)
                        .count())
                .crashBuyCount(getRuleCount(ruleApplicationCounts, BehaviorRuleCode.CRASH_BUY))
                .crashFullSellCount(getRuleCount(
                        ruleApplicationCounts,
                        BehaviorRuleCode.CRASH_FULL_SELL
                ))
                .bullBuyCount(getRuleCount(ruleApplicationCounts, BehaviorRuleCode.BULL_BUY))
                .bullProfitSellCount(getRuleCount(
                        ruleApplicationCounts,
                        BehaviorRuleCode.BULL_PROFIT_SELL
                ))
                .lossAveragingBuyCount(getRuleCount(
                        ruleApplicationCounts,
                        BehaviorRuleCode.LOSS_AVERAGING_BUY
                ))
                .lossCutSellCount(getRuleCount(
                        ruleApplicationCounts,
                        BehaviorRuleCode.LOSS_CUT_SELL
                ))
                .crashHoldingEpisodeCount(crashHoldingEpisodeCount)
                .normalPlannedBuyCount(normalPlannedBuyCount)
                .cashBufferMaintenanceCount(cashBufferMaintenanceCount)
                .depositCancelled(actions.stream()
                        .anyMatch(action -> action.getActionType() == BehaviorActionType.CANCEL_PRODUCT))
                .depositMatured(actions.stream()
                        .anyMatch(action -> action.getActionType() == BehaviorActionType.MATURITY))
                .boughtStockAfterDepositCancel(getRuleCount(
                        ruleApplicationCounts,
                        BehaviorRuleCode.DEPOSIT_CANCEL_AND_SECURITY_BUY
                ) > 0)
                .maximumConsecutiveBuyCount(calculateMaximumConsecutiveActionCount(
                        behaviorContexts,
                        BehaviorActionType.BUY
                ))
                .maximumConsecutiveSellCount(calculateMaximumConsecutiveActionCount(
                        behaviorContexts,
                        BehaviorActionType.SELL
                ))
                .consecutiveActionLevelTwoCount(countConsecutiveActions(
                        behaviorContexts,
                        2,
                        false
                ))
                .consecutiveActionLevelThreeOrMoreCount(countConsecutiveActions(
                        behaviorContexts,
                        3,
                        true
                ))
                .ruleApplicationCounts(ruleApplicationCounts)
                .ruleScoreContributions(ruleScoreContributions)
                .finalRtScore(assessmentScore.getRtScore())
                .finalLhScore(assessmentScore.getLhScore())
                .finalRpScore(assessmentScore.getRpScore())
                .personaType(personaClassifier.calculatePersona(assessmentScore))
                .build();
    }

    int calculateCrashHoldingEpisodeCount(
            ScenarioDto scenario,
            int initialStockQuantity,
            List<SimulatedGameAction> actions) {
        int currentStockQuantity = initialStockQuantity;
        int episodeStartQuantity = 0;
        int episodeEndQuantity = 0;
        int maintainedEpisodeCount = 0;
        boolean crashEpisode = false;

        Map<Integer, List<SimulatedGameAction>> actionsByTick = actions.stream()
                .filter(action -> action.getActionType() != BehaviorActionType.MATURITY)
                .collect(java.util.stream.Collectors.groupingBy(
                        SimulatedGameAction::getGameTick,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        List<ScenarioTickDto> scenarioTicks = scenario.getTicks().stream()
                .filter(tick -> tick.getTick() >= 0 && tick.getTick() < scenario.getTotalTicks())
                .sorted(java.util.Comparator.comparingInt(ScenarioTickDto::getTick))
                .toList();

        for (ScenarioTickDto scenarioTick : scenarioTicks) {
            MarketState marketState = marketStateCalculator.calculateMarketState(
                    gamePriceRateCalculator.calculateTickPriceChangeRate(
                            scenario,
                            scenarioTick.getTick()
                    ),
                    null
            );
            if (marketState == MarketState.CRASH && !crashEpisode) {
                crashEpisode = true;
                episodeStartQuantity = currentStockQuantity;
            } else if (marketState != MarketState.CRASH && crashEpisode) {
                if (isCrashHoldingMaintained(episodeStartQuantity, episodeEndQuantity)) {
                    maintainedEpisodeCount++;
                }
                crashEpisode = false;
            }

            for (SimulatedGameAction action : actionsByTick.getOrDefault(
                    scenarioTick.getTick(),
                    List.of()
            )) {
                currentStockQuantity = action.getCurrentStockQuantity();
            }
            if (crashEpisode) {
                episodeEndQuantity = currentStockQuantity;
            }
        }
        if (crashEpisode && isCrashHoldingMaintained(episodeStartQuantity, episodeEndQuantity)) {
            maintainedEpisodeCount++;
        }
        return maintainedEpisodeCount;
    }

    private boolean isCrashHoldingMaintained(int startQuantity, int endQuantity) {
        if (startQuantity <= 0 || endQuantity <= 0) {
            return false;
        }
        return BigDecimal.valueOf(endQuantity)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(startQuantity), 4, RoundingMode.HALF_UP)
                .compareTo(CRASH_HOLDING_MINIMUM_RATIO) >= 0;
    }

    int calculateNormalPlannedBuyCount(
            List<BehaviorContext> behaviorContexts,
            List<BehaviorAnalysisResult> analysisResults) {
        if (behaviorContexts.size() != analysisResults.size()) {
            throw new IllegalArgumentException("행동 조건과 분석 결과의 개수가 일치해야 합니다.");
        }
        Set<Integer> appliedTicks = new HashSet<>();
        for (int index = 0; index < behaviorContexts.size(); index++) {
            BehaviorContext behaviorContext = behaviorContexts.get(index);
            BehaviorEvent behaviorEvent = behaviorContext.getCurrentEvent();
            if (behaviorEvent == null
                    || behaviorEvent.getGameTick() == null
                    || behaviorEvent.getActionType() != BehaviorActionType.BUY
                    || behaviorEvent.getAssetType() != BehaviorAssetType.SECURITY
                    || behaviorContext.getMarketState() != MarketState.NORMAL
                    || containsRule(
                    analysisResults.get(index),
                    BehaviorRuleCode.LOSS_AVERAGING_BUY
            )) {
                continue;
            }
            BigDecimal buyRatio = calculateActionAmountRatio(behaviorEvent);
            if (buyRatio.compareTo(NORMAL_BUY_MINIMUM_RATIO) >= 0
                    && buyRatio.compareTo(NORMAL_BUY_MAXIMUM_RATIO) < 0) {
                appliedTicks.add(behaviorEvent.getGameTick());
            }
        }
        return appliedTicks.size();
    }

    private boolean containsRule(
            BehaviorAnalysisResult analysisResult,
            BehaviorRuleCode ruleCode) {
        return analysisResult.getAppliedRules().stream()
                .anyMatch(ruleResult -> ruleResult.getRuleCode() == ruleCode);
    }

    private BigDecimal calculateActionAmountRatio(BehaviorEvent behaviorEvent) {
        if (behaviorEvent.getActionAmount() == null || behaviorEvent.getActionAmount() <= 0) {
            return BigDecimal.ZERO;
        }
        long totalPrincipal = Math.addExact(
                Math.addExact(
                        behaviorEvent.getCurrentCash(),
                        behaviorEvent.getCurrentStockPrincipal()
                ),
                behaviorEvent.getCurrentDeposit()
        );
        if (totalPrincipal <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(behaviorEvent.getActionAmount())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalPrincipal), 4, RoundingMode.HALF_UP);
    }

    int calculateCashBufferMaintenanceCount(
            ScenarioDto scenario,
            long initialCash,
            long initialStockPrincipal,
            long initialDeposit,
            List<SimulatedGameAction> actions) {
        long currentCash = initialCash;
        long currentStockPrincipal = initialStockPrincipal;
        long currentDeposit = initialDeposit;
        int consecutiveMaintenanceTicks = 0;

        Map<Integer, List<SimulatedGameAction>> actionsByTick = actions.stream()
                .filter(action -> action.getActionType() != BehaviorActionType.MATURITY)
                .collect(java.util.stream.Collectors.groupingBy(
                        SimulatedGameAction::getGameTick,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        List<ScenarioTickDto> scenarioTicks = scenario.getTicks().stream()
                .filter(tick -> tick.getTick() >= 0 && tick.getTick() < scenario.getTotalTicks())
                .sorted(java.util.Comparator.comparingInt(ScenarioTickDto::getTick))
                .toList();

        for (ScenarioTickDto scenarioTick : scenarioTicks) {
            for (SimulatedGameAction action : actionsByTick.getOrDefault(
                    scenarioTick.getTick(),
                    List.of()
            )) {
                currentCash = action.getCurrentCash();
                currentStockPrincipal = action.getCurrentStockPrincipal();
                currentDeposit = action.getCurrentDeposit();
            }
            BigDecimal cashRatio = assetRatioCalculator.calculateCashRatio(
                    currentCash,
                    currentStockPrincipal,
                    currentDeposit
            );
            if (cashRatio.compareTo(CASH_BUFFER_MINIMUM_RATIO) >= 0
                    && cashRatio.compareTo(CASH_BUFFER_MAXIMUM_RATIO) < 0) {
                consecutiveMaintenanceTicks++;
                if (consecutiveMaintenanceTicks >= CASH_BUFFER_MAINTENANCE_TICKS) {
                    return 1;
                }
            } else {
                consecutiveMaintenanceTicks = 0;
            }
        }
        return 0;
    }

    int calculateCashBufferMaintenanceEpisodeCount(
            ScenarioDto scenario,
            long initialCash,
            long initialStockPrincipal,
            long initialDeposit,
            List<SimulatedGameAction> actions) {
        long currentCash = initialCash;
        long currentStockPrincipal = initialStockPrincipal;
        long currentDeposit = initialDeposit;
        int consecutiveMaintenanceTicks = 0;
        int maintenanceEpisodeCount = 0;
        boolean currentEpisodeApplied = false;

        Map<Integer, List<SimulatedGameAction>> actionsByTick = actions.stream()
                .filter(action -> action.getActionType() != BehaviorActionType.MATURITY)
                .collect(java.util.stream.Collectors.groupingBy(
                        SimulatedGameAction::getGameTick,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        List<ScenarioTickDto> scenarioTicks = scenario.getTicks().stream()
                .filter(tick -> tick.getTick() >= 0 && tick.getTick() < scenario.getTotalTicks())
                .sorted(java.util.Comparator.comparingInt(ScenarioTickDto::getTick))
                .toList();

        for (ScenarioTickDto scenarioTick : scenarioTicks) {
            for (SimulatedGameAction action : actionsByTick.getOrDefault(
                    scenarioTick.getTick(),
                    List.of()
            )) {
                currentCash = action.getCurrentCash();
                currentStockPrincipal = action.getCurrentStockPrincipal();
                currentDeposit = action.getCurrentDeposit();
            }
            BigDecimal cashRatio = assetRatioCalculator.calculateCashRatio(
                    currentCash,
                    currentStockPrincipal,
                    currentDeposit
            );
            boolean maintainsCashBuffer =
                    cashRatio.compareTo(CASH_BUFFER_MINIMUM_RATIO) >= 0
                            && cashRatio.compareTo(CASH_BUFFER_MAXIMUM_RATIO) < 0;
            if (!maintainsCashBuffer) {
                consecutiveMaintenanceTicks = 0;
                currentEpisodeApplied = false;
                continue;
            }

            consecutiveMaintenanceTicks++;
            if (!currentEpisodeApplied
                    && consecutiveMaintenanceTicks >= CASH_BUFFER_MAINTENANCE_TICKS) {
                maintenanceEpisodeCount++;
                currentEpisodeApplied = true;
            }
        }
        return maintenanceEpisodeCount;
    }

    private int countActions(
            List<SimulatedGameAction> actions,
            BehaviorActionType actionType) {
        return (int) actions.stream()
                .filter(action -> action.getActionType() == actionType)
                .count();
    }

    private long calculateTotalActionAmount(
            List<SimulatedGameAction> actions,
            BehaviorActionType actionType) {
        return actions.stream()
                .filter(action -> action.getActionType() == actionType)
                .mapToLong(SimulatedGameAction::getActionAmount)
                .sum();
    }

    private int calculateMaximumConsecutiveActionCount(
            List<BehaviorContext> behaviorContexts,
            BehaviorActionType actionType) {
        return behaviorContexts.stream()
                .filter(context -> context.getCurrentEvent() != null)
                .filter(context -> context.getCurrentEvent().getActionType() == actionType)
                .map(BehaviorContext::getConsecutiveActionCount)
                .filter(count -> count != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    private int countConsecutiveActions(
            List<BehaviorContext> behaviorContexts,
            int consecutiveActionCount,
            boolean includeGreaterCount) {
        return (int) behaviorContexts.stream()
                .filter(context -> context.getCurrentEvent() != null)
                .filter(context -> context.getCurrentEvent().getActionType() == BehaviorActionType.BUY
                        || context.getCurrentEvent().getActionType() == BehaviorActionType.SELL)
                .map(BehaviorContext::getConsecutiveActionCount)
                .filter(count -> count != null)
                .filter(count -> includeGreaterCount
                        ? count >= consecutiveActionCount
                        : count == consecutiveActionCount)
                .count();
    }

    private int getRuleCount(
            Map<BehaviorRuleCode, Integer> ruleApplicationCounts,
            BehaviorRuleCode ruleCode) {
        return ruleApplicationCounts.getOrDefault(ruleCode, 0);
    }

    private long addAmounts(long firstAmount, long secondAmount) {
        try {
            return Math.addExact(firstAmount, secondAmount);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("초기 자산 금액이 허용 범위를 초과했습니다.", exception);
        }
    }

    private void validateSimulationInput(
            long simulationUserId,
            ScenarioDto scenario,
            SimulatedGamePortfolio initialPortfolio) {
        if (simulationUserId <= 0) {
            throw new IllegalArgumentException("시뮬레이션 사용자 ID는 0보다 커야 합니다.");
        }
        if (scenario == null) {
            throw new IllegalArgumentException("게임 시나리오는 필수입니다.");
        }
        if (initialPortfolio == null) {
            throw new IllegalArgumentException("초기 자산 상태는 필수입니다.");
        }
    }
}
