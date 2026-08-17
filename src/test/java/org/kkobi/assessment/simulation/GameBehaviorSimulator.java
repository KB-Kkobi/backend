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
    private static final BigDecimal RISK_BUDGET_STOCK_MINIMUM_RATIO =
            BigDecimal.valueOf(50);
    private static final BigDecimal RISK_BUDGET_STOCK_MAXIMUM_RATIO =
            BigDecimal.valueOf(70);
    private static final BigDecimal RISK_BUDGET_CASH_MAXIMUM_RATIO =
            BigDecimal.valueOf(40);
    private static final BigDecimal PASSIVE_HIGH_RISK_STOCK_MINIMUM_RATIO =
            BigDecimal.valueOf(70);
    private static final BigDecimal PASSIVE_HIGH_RISK_CASH_MAXIMUM_RATIO =
            BigDecimal.valueOf(20);
    private static final int CANDIDATE_MAINTENANCE_TICKS = 3;
    private static final int HIGH_STOCK_INACTIVITY_TICKS = 5;
    private static final BigDecimal BULL_EXPOSURE_RETENTION_RATIO =
            BigDecimal.valueOf(80);
    private static final BigDecimal REFINED_NO_CHASE_STOCK_MINIMUM_RATIO =
            BigDecimal.valueOf(60);
    private static final BigDecimal REFINED_NO_CHASE_LIQUID_ASSET_MAXIMUM_RATIO =
            BigDecimal.valueOf(20);
    private static final BigDecimal LHH_LIQUID_ASSET_MINIMUM_RATIO =
            BigDecimal.valueOf(60);
    private static final BigDecimal LHH_STOCK_MAXIMUM_RATIO =
            BigDecimal.valueOf(40);
    private static final BigDecimal TARGETED_LHH_LIQUID_ASSET_MINIMUM_RATIO =
            BigDecimal.valueOf(70);
    private static final BigDecimal TARGETED_LHH_STOCK_MAXIMUM_RATIO =
            BigDecimal.valueOf(30);
    private static final int OPPORTUNITY_REALIZATION_TICKS = 10;
    private static final ScoreDelta RISK_BUDGET_MAINTENANCE_SCORE =
            ScoreDelta.createScoreDelta(5, 5, -5);
    private static final ScoreDelta PASSIVE_HIGH_RISK_HOLDING_SCORE =
            ScoreDelta.createScoreDelta(5, -5, -5);
    private static final ScoreDelta LIQUIDITY_PRESERVING_OPPORTUNITY_SCORE =
            ScoreDelta.createScoreDelta(0, 5, 5);
    private static final ScoreDelta RISK_EXPOSURE_NO_CHASE_SCORE =
            ScoreDelta.createScoreDelta(5, 0, -5);
    private static final ScoreDelta HIGH_STOCK_LOW_CASH_INACTIVITY_SCORE =
            ScoreDelta.createScoreDelta(5, -5, -5);
    private static final ScoreDelta LHH_LIQUIDITY_OPPORTUNITY_SCORE =
            ScoreDelta.createScoreDelta(-5, 5, 5);
    private static final ScoreDelta HHL_COMPOSITE_SCORE =
            ScoreDelta.createScoreDelta(5, 5, -5);
    private static final ScoreDelta HLL_CAPPED_NO_CHASE_SCORE =
            ScoreDelta.createScoreDelta(5, -5, -5);
    private static final ScoreDelta LHH_COMPLETED_OPPORTUNITY_SCORE =
            ScoreDelta.createScoreDelta(0, 0, 5);
    private static final int CANDIDATE_RULE_P95 = 4;
    private static final int CRASH_HOLDING_P95 = 4;
    private static final int NORMAL_PLANNED_BUY_P95 = 9;
    private static final int CASH_BUFFER_MAINTENANCE_P95 = 4;
    private static final int BUY_GROUP_P95 = 19;
    private static final int SELL_GROUP_P95 = 9;
    private static final int STATE_MAINTENANCE_GROUP_P95 = 8;
    private static final BigDecimal BUY_GROUP_MAXIMUM_MULTIPLIER = BigDecimal.valueOf(2.5);
    private static final BigDecimal SELL_GROUP_MAXIMUM_MULTIPLIER = BigDecimal.valueOf(2.5);
    private static final BigDecimal STATE_GROUP_MAXIMUM_MULTIPLIER = BigDecimal.valueOf(1.5);
    private static final Map<BehaviorRuleCode, Integer> REPEATED_RULE_P95 = Map.of(
            BehaviorRuleCode.CRASH_BUY, 4,
            BehaviorRuleCode.CRASH_FULL_SELL, 1,
            BehaviorRuleCode.BULL_BUY, 4,
            BehaviorRuleCode.BULL_PROFIT_SELL, 4,
            BehaviorRuleCode.LOSS_AVERAGING_BUY, 2,
            BehaviorRuleCode.LOSS_CUT_SELL, 4
    );
    private static final int OPPORTUNITY_CONFIDENCE_K = 1;
    private static final Set<BehaviorRuleCode> ONE_TIME_GAME_RULE_CODES = Set.of(
            BehaviorRuleCode.INITIAL_STOCK_ALLOCATION,
            BehaviorRuleCode.INITIAL_DEPOSIT_ALLOCATION,
            BehaviorRuleCode.INITIAL_CASH_ALLOCATION,
            BehaviorRuleCode.DEPOSIT_CANCEL_AND_SECURITY_BUY,
            BehaviorRuleCode.DEPOSIT_MATURITY
    );

    private final GameBehaviorGenerator behaviorGenerator;
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

    public GameBehaviorSimulator() {
        this(new NeutralGameBehaviorGenerator());
    }

    GameBehaviorSimulator(GameBehaviorGenerator behaviorGenerator) {
        if (behaviorGenerator == null) {
            throw new IllegalArgumentException("행동 생성기는 필수입니다.");
        }
        this.behaviorGenerator = behaviorGenerator;
    }

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
        int riskBudgetMaintenanceCount =
                ruleEvaluationCondition.appliesRiskBudgetMaintenanceRule()
                        ? calculateRiskBudgetMaintenanceEpisodeCount(
                                scenario,
                                initialCash,
                                initialStockPrincipal,
                                initialDeposit,
                                generationResult.getActions()
                        )
                        : 0;
        int passiveHighRiskHoldingCount =
                ruleEvaluationCondition.appliesPassiveHighRiskHoldingRule()
                        ? calculatePassiveHighRiskHoldingEpisodeCount(
                                scenario,
                                initialCash,
                                initialStockPrincipal,
                                initialDeposit,
                                generationResult.getActions()
                        )
                        : 0;
        int liquidityPreservingOpportunityCount =
                ruleEvaluationCondition.appliesLiquidityPreservingOpportunityRule()
                        ? calculateLiquidityPreservingOpportunityCount(
                                behaviorContexts,
                                analysisResults
                        )
                        : 0;
        int riskExposureNoChaseCount =
                ruleEvaluationCondition.appliesRiskExposureNoChaseRule()
                        ? calculateRiskExposureNoChaseEpisodeCount(
                                scenario,
                                initialCash,
                                initialStockPrincipal,
                                initialDeposit,
                                initialStockQuantity,
                                generationResult.getActions()
                        )
                        : 0;
        int highStockLowCashInactivityCount =
                ruleEvaluationCondition.appliesHighStockLowCashInactivityRule()
                        ? calculateHighStockLowCashInactivityEpisodeCount(
                                scenario,
                                initialCash,
                                initialStockPrincipal,
                                initialDeposit,
                                generationResult.getActions()
                        )
                        : 0;
        Set<Integer> lhhLiquidityOpportunityTicks =
                ruleEvaluationCondition.appliesLhhLiquidityOpportunityRule()
                        ? calculateLhhLiquidityOpportunityTicks(
                                behaviorContexts,
                                analysisResults
                        )
                        : Set.of();
        int refinedRiskExposureNoChaseCount =
                ruleEvaluationCondition.appliesRefinedRiskExposureNoChaseRule()
                        ? calculateRiskExposureNoChaseEpisodeCount(
                                scenario,
                                initialCash,
                                initialStockPrincipal,
                                initialDeposit,
                                initialStockQuantity,
                                generationResult.getActions(),
                                true,
                                ruleEvaluationCondition.appliesMutuallyExclusiveCandidateRules()
                                        ? lhhLiquidityOpportunityTicks
                                        : Set.of()
                        )
                        : 0;
        int hhlCompositeCount = ruleEvaluationCondition.appliesHhlCompositeRule()
                ? calculateHhlCompositeEpisodeCount(
                        scenario,
                        initialCash,
                        initialStockPrincipal,
                        initialDeposit,
                        initialStockQuantity,
                        generationResult.getActions()
                )
                : 0;
        int hllCappedNoChaseCount = ruleEvaluationCondition.appliesHllCappedNoChaseRule()
                ? Math.min(1, calculateRiskExposureNoChaseEpisodeCount(
                        scenario,
                        initialCash,
                        initialStockPrincipal,
                        initialDeposit,
                        initialStockQuantity,
                        generationResult.getActions(),
                        true,
                        Set.of()
                ))
                : 0;
        int lhhCompletedOpportunityCount =
                ruleEvaluationCondition.appliesLhhCompletedOpportunityRule()
                        ? calculateLhhCompletedOpportunityCount(
                                behaviorContexts,
                                analysisResults
                        )
                        : 0;
        List<ScoreDelta> scoreDeltas;
        if (ruleEvaluationCondition.appliesLogDiminishingRuleGroupScore()) {
            scoreDeltas = calculateLogDiminishingRuleGroupScores(
                    analysisResults,
                    crashHoldingEpisodeCount,
                    normalPlannedBuyCount,
                    cashBufferMaintenanceCount,
                    ruleEvaluationCondition.getBuyGroupMaximumMultiplier(),
                    ruleEvaluationCondition.getSellGroupMaximumMultiplier(),
                    ruleEvaluationCondition.getStateGroupMaximumMultiplier()
            );
        } else if (ruleEvaluationCondition.appliesOpportunityWeightedRepeatedScore()) {
            scoreDeltas = calculateOpportunityWeightedScoreDeltas(
                        scenario,
                        behaviorContexts,
                        analysisResults
            );
        } else if (ruleEvaluationCondition.appliesLogDiminishingRepeatedRuleScore()) {
            scoreDeltas = new ArrayList<>(calculateLogDiminishingRuleContributions(
                    analysisResults
            ).values());
        } else {
            scoreDeltas = new ArrayList<>(analysisResults.stream()
                    .map(BehaviorAnalysisResult::getTotalScoreDelta)
                    .toList());
        }
        if (!ruleEvaluationCondition.appliesLogDiminishingRuleGroupScore()) {
            if (ruleEvaluationCondition.appliesLogDiminishingCandidateScore()) {
                scoreDeltas.add(calculateLogDiminishingCandidateScore(
                        CRASH_HOLDING_SCORE,
                        crashHoldingEpisodeCount,
                        CRASH_HOLDING_P95
                ));
                scoreDeltas.add(calculateLogDiminishingCandidateScore(
                        NORMAL_PLANNED_BUY_SCORE,
                        normalPlannedBuyCount,
                        NORMAL_PLANNED_BUY_P95
                ));
                scoreDeltas.add(calculateLogDiminishingCandidateScore(
                        CASH_BUFFER_MAINTENANCE_SCORE,
                        cashBufferMaintenanceCount,
                        CASH_BUFFER_MAINTENANCE_P95
                ));
            } else {
                addRepeatedScore(scoreDeltas, CRASH_HOLDING_SCORE, crashHoldingEpisodeCount);
                addRepeatedScore(scoreDeltas, NORMAL_PLANNED_BUY_SCORE, normalPlannedBuyCount);
                addRepeatedScore(
                        scoreDeltas,
                        CASH_BUFFER_MAINTENANCE_SCORE,
                        cashBufferMaintenanceCount
                );
            }
        }
        if (ruleEvaluationCondition.appliesRiskBudgetMaintenanceRule()) {
            scoreDeltas.add(calculateLogDiminishingCandidateScore(
                    RISK_BUDGET_MAINTENANCE_SCORE,
                    riskBudgetMaintenanceCount,
                    CANDIDATE_RULE_P95
            ));
        }
        if (ruleEvaluationCondition.appliesPassiveHighRiskHoldingRule()) {
            scoreDeltas.add(calculateLogDiminishingCandidateScore(
                    PASSIVE_HIGH_RISK_HOLDING_SCORE,
                    passiveHighRiskHoldingCount,
                    CANDIDATE_RULE_P95
            ));
        }
        if (ruleEvaluationCondition.appliesLiquidityPreservingOpportunityRule()) {
            scoreDeltas.add(calculateLogDiminishingCandidateScore(
                    LIQUIDITY_PRESERVING_OPPORTUNITY_SCORE,
                    liquidityPreservingOpportunityCount,
                    CANDIDATE_RULE_P95
            ));
        }
        if (ruleEvaluationCondition.appliesRiskExposureNoChaseRule()) {
            scoreDeltas.add(calculateLogDiminishingCandidateScore(
                    RISK_EXPOSURE_NO_CHASE_SCORE,
                    riskExposureNoChaseCount,
                    CANDIDATE_RULE_P95
            ));
        }
        if (ruleEvaluationCondition.appliesHighStockLowCashInactivityRule()) {
            scoreDeltas.add(calculateLogDiminishingCandidateScore(
                    HIGH_STOCK_LOW_CASH_INACTIVITY_SCORE,
                    highStockLowCashInactivityCount,
                    CANDIDATE_RULE_P95
            ));
        }
        if (ruleEvaluationCondition.appliesRefinedRiskExposureNoChaseRule()) {
            scoreDeltas.add(calculateLogDiminishingCandidateScore(
                    RISK_EXPOSURE_NO_CHASE_SCORE,
                    refinedRiskExposureNoChaseCount,
                    CANDIDATE_RULE_P95
            ));
        }
        if (ruleEvaluationCondition.appliesLhhLiquidityOpportunityRule()) {
            scoreDeltas.add(calculateLogDiminishingCandidateScore(
                    LHH_LIQUIDITY_OPPORTUNITY_SCORE,
                    lhhLiquidityOpportunityTicks.size(),
                    CANDIDATE_RULE_P95
            ));
        }
        if (ruleEvaluationCondition.appliesHhlCompositeRule()) {
            scoreDeltas.add(calculateLogDiminishingCandidateScore(
                    HHL_COMPOSITE_SCORE,
                    hhlCompositeCount,
                    1
            ));
        }
        if (ruleEvaluationCondition.appliesHllCappedNoChaseRule()) {
            scoreDeltas.add(calculateLogDiminishingCandidateScore(
                    HLL_CAPPED_NO_CHASE_SCORE,
                    hllCappedNoChaseCount,
                    1
            ));
        }
        if (ruleEvaluationCondition.appliesLhhCompletedOpportunityRule()) {
            scoreDeltas.add(calculateLogDiminishingCandidateScore(
                    LHH_COMPLETED_OPPORTUNITY_SCORE,
                    lhhCompletedOpportunityCount,
                    1
            ));
        }
        AssessmentScore assessmentScore = gameScoreCalculator.calculateGameScore(scoreDeltas);
        Map<BehaviorRuleCode, Integer> ruleApplicationCounts = calculateRuleApplicationCounts(
                analysisResults
        );
        Map<BehaviorRuleCode, ScoreDelta> ruleScoreContributions;
        if (ruleEvaluationCondition.appliesOpportunityWeightedRepeatedScore()) {
            ruleScoreContributions = calculateOpportunityWeightedRuleContributions(
                        scenario,
                        behaviorContexts,
                        analysisResults
            );
        } else if (ruleEvaluationCondition.appliesLogDiminishingRepeatedRuleScore()) {
            ruleScoreContributions = calculateLogDiminishingRuleContributions(analysisResults);
        } else {
            ruleScoreContributions = calculateRuleScoreContributions(analysisResults);
        }

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

    List<ScoreDelta> calculateOpportunityWeightedScoreDeltas(
            ScenarioDto scenario,
            List<BehaviorContext> behaviorContexts,
            List<BehaviorAnalysisResult> analysisResults) {
        return new ArrayList<>(calculateOpportunityWeightedRuleContributions(
                scenario,
                behaviorContexts,
                analysisResults
        ).values());
    }

    ScoreDelta calculateLogDiminishingCandidateScore(
            ScoreDelta maximumScore,
            int applicationCount,
            int p95ApplicationCount) {
        if (applicationCount <= 0) {
            return ScoreDelta.createZeroScoreDelta();
        }
        if (p95ApplicationCount <= 0) {
            throw new IllegalArgumentException("P95 적용 횟수는 1 이상이어야 합니다.");
        }

        int effectiveCount = Math.min(applicationCount, p95ApplicationCount);
        BigDecimal weight = BigDecimal.valueOf(
                Math.log1p(effectiveCount) / Math.log1p(p95ApplicationCount)
        );
        return maximumScore.multiplyScoreDelta(weight);
    }

    Map<BehaviorRuleCode, ScoreDelta> calculateLogDiminishingRuleContributions(
            List<BehaviorAnalysisResult> analysisResults) {
        EnumMap<BehaviorRuleCode, ScoreDelta> scoreSums =
                new EnumMap<>(BehaviorRuleCode.class);
        EnumMap<BehaviorRuleCode, Integer> applicationCounts =
                new EnumMap<>(BehaviorRuleCode.class);

        for (BehaviorAnalysisResult analysisResult : analysisResults) {
            for (RuleResult ruleResult : analysisResult.getAppliedRules()) {
                scoreSums.merge(
                        ruleResult.getRuleCode(),
                        ruleResult.getScoreDelta(),
                        ScoreDelta::addScoreDelta
                );
                applicationCounts.merge(ruleResult.getRuleCode(), 1, Integer::sum);
            }
        }

        EnumMap<BehaviorRuleCode, ScoreDelta> contributions =
                new EnumMap<>(BehaviorRuleCode.class);
        scoreSums.forEach((ruleCode, scoreSum) -> {
            int applicationCount = applicationCounts.get(ruleCode);
            if (ONE_TIME_GAME_RULE_CODES.contains(ruleCode)) {
                contributions.put(ruleCode, scoreSum);
                return;
            }
            int p95ApplicationCount = REPEATED_RULE_P95.getOrDefault(
                    ruleCode,
                    Math.max(1, applicationCount)
            );
            ScoreDelta averageScore = divideScoreDelta(
                    scoreSum,
                    BigDecimal.valueOf(applicationCount)
            );
            contributions.put(
                    ruleCode,
                    calculateLogDiminishingCandidateScore(
                            averageScore,
                            applicationCount,
                            p95ApplicationCount
                    )
            );
        });
        return contributions;
    }

    List<ScoreDelta> calculateLogDiminishingRuleGroupScores(
            List<BehaviorAnalysisResult> analysisResults,
            int crashHoldingEpisodeCount,
            int normalPlannedBuyCount,
            int cashBufferMaintenanceCount) {
        return calculateLogDiminishingRuleGroupScores(
                analysisResults,
                crashHoldingEpisodeCount,
                normalPlannedBuyCount,
                cashBufferMaintenanceCount,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE
        );
    }

    List<ScoreDelta> calculateLogDiminishingRuleGroupScores(
            List<BehaviorAnalysisResult> analysisResults,
            int crashHoldingEpisodeCount,
            int normalPlannedBuyCount,
            int cashBufferMaintenanceCount,
            boolean appliesBalancedMaximum) {
        return calculateLogDiminishingRuleGroupScores(
                analysisResults,
                crashHoldingEpisodeCount,
                normalPlannedBuyCount,
                cashBufferMaintenanceCount,
                appliesBalancedMaximum ? BUY_GROUP_MAXIMUM_MULTIPLIER : BigDecimal.ONE,
                appliesBalancedMaximum ? SELL_GROUP_MAXIMUM_MULTIPLIER : BigDecimal.ONE,
                appliesBalancedMaximum ? STATE_GROUP_MAXIMUM_MULTIPLIER : BigDecimal.ONE
        );
    }

    List<ScoreDelta> calculateLogDiminishingRuleGroupScores(
            List<BehaviorAnalysisResult> analysisResults,
            int crashHoldingEpisodeCount,
            int normalPlannedBuyCount,
            int cashBufferMaintenanceCount,
            BigDecimal buyMaximumMultiplier,
            BigDecimal sellMaximumMultiplier,
            BigDecimal stateMaximumMultiplier) {
        List<ScoreDelta> oneTimeScores = new ArrayList<>();
        EnumMap<RepeatedRuleGroup, ScoreDelta> groupScoreSums =
                new EnumMap<>(RepeatedRuleGroup.class);
        EnumMap<RepeatedRuleGroup, Integer> groupApplicationCounts =
                new EnumMap<>(RepeatedRuleGroup.class);

        for (BehaviorAnalysisResult analysisResult : analysisResults) {
            for (RuleResult ruleResult : analysisResult.getAppliedRules()) {
                BehaviorRuleCode ruleCode = ruleResult.getRuleCode();
                if (ONE_TIME_GAME_RULE_CODES.contains(ruleCode)) {
                    oneTimeScores.add(ruleResult.getScoreDelta());
                    continue;
                }
                RepeatedRuleGroup ruleGroup = RepeatedRuleGroup.from(ruleCode);
                if (ruleGroup == null) {
                    oneTimeScores.add(ruleResult.getScoreDelta());
                    continue;
                }
                mergeGroupScore(
                        groupScoreSums,
                        groupApplicationCounts,
                        ruleGroup,
                        ruleResult.getScoreDelta(),
                        1
                );
            }
        }

        mergeGroupScore(
                groupScoreSums,
                groupApplicationCounts,
                RepeatedRuleGroup.BUY,
                NORMAL_PLANNED_BUY_SCORE.multiplyScoreDelta(
                        BigDecimal.valueOf(normalPlannedBuyCount)
                ),
                normalPlannedBuyCount
        );
        mergeGroupScore(
                groupScoreSums,
                groupApplicationCounts,
                RepeatedRuleGroup.STATE_MAINTENANCE,
                CRASH_HOLDING_SCORE.multiplyScoreDelta(
                        BigDecimal.valueOf(crashHoldingEpisodeCount)
                ).addScoreDelta(CASH_BUFFER_MAINTENANCE_SCORE.multiplyScoreDelta(
                        BigDecimal.valueOf(cashBufferMaintenanceCount)
                )),
                crashHoldingEpisodeCount + cashBufferMaintenanceCount
        );

        List<ScoreDelta> scores = new ArrayList<>(oneTimeScores);
        groupScoreSums.forEach((ruleGroup, scoreSum) -> {
            int applicationCount = groupApplicationCounts.get(ruleGroup);
            ScoreDelta averageScore = divideScoreDelta(
                    scoreSum,
                    BigDecimal.valueOf(applicationCount)
            );
            ScoreDelta groupScore = calculateLogDiminishingCandidateScore(
                    averageScore,
                    applicationCount,
                    ruleGroup.getP95ApplicationCount()
            );
            scores.add(groupScore.multiplyScoreDelta(ruleGroup.selectMaximumMultiplier(
                    buyMaximumMultiplier,
                    sellMaximumMultiplier,
                    stateMaximumMultiplier
            )));
        });
        return scores;
    }

    private void mergeGroupScore(
            Map<RepeatedRuleGroup, ScoreDelta> scoreSums,
            Map<RepeatedRuleGroup, Integer> applicationCounts,
            RepeatedRuleGroup ruleGroup,
            ScoreDelta scoreDelta,
            int applicationCount) {
        if (applicationCount <= 0) {
            return;
        }
        scoreSums.merge(ruleGroup, scoreDelta, ScoreDelta::addScoreDelta);
        applicationCounts.merge(ruleGroup, applicationCount, Integer::sum);
    }

    private enum RepeatedRuleGroup {
        BUY(BUY_GROUP_P95, BUY_GROUP_MAXIMUM_MULTIPLIER),
        SELL(SELL_GROUP_P95, SELL_GROUP_MAXIMUM_MULTIPLIER),
        STATE_MAINTENANCE(STATE_MAINTENANCE_GROUP_P95, STATE_GROUP_MAXIMUM_MULTIPLIER);

        private final int p95ApplicationCount;
        private final BigDecimal maximumMultiplier;

        RepeatedRuleGroup(int p95ApplicationCount, BigDecimal maximumMultiplier) {
            this.p95ApplicationCount = p95ApplicationCount;
            this.maximumMultiplier = maximumMultiplier;
        }

        int getP95ApplicationCount() {
            return p95ApplicationCount;
        }

        BigDecimal getMaximumMultiplier() {
            return maximumMultiplier;
        }

        BigDecimal selectMaximumMultiplier(
                BigDecimal buyMaximumMultiplier,
                BigDecimal sellMaximumMultiplier,
                BigDecimal stateMaximumMultiplier) {
            return switch (this) {
                case BUY -> buyMaximumMultiplier;
                case SELL -> sellMaximumMultiplier;
                case STATE_MAINTENANCE -> stateMaximumMultiplier;
            };
        }

        static RepeatedRuleGroup from(BehaviorRuleCode ruleCode) {
            return switch (ruleCode) {
                case CRASH_BUY, BULL_BUY, LOSS_AVERAGING_BUY -> BUY;
                case CRASH_FULL_SELL, BULL_PROFIT_SELL, LOSS_CUT_SELL -> SELL;
                case VERY_LOW_CASH_MAINTENANCE,
                        MEDIUM_CASH_MAINTENANCE,
                        HIGH_CASH_MAINTENANCE -> STATE_MAINTENANCE;
                default -> null;
            };
        }
    }

    private void addRepeatedScore(
            List<ScoreDelta> scoreDeltas,
            ScoreDelta scoreDelta,
            int applicationCount) {
        for (int count = 0; count < applicationCount; count++) {
            scoreDeltas.add(scoreDelta);
        }
    }

    private Map<BehaviorRuleCode, ScoreDelta> calculateOpportunityWeightedRuleContributions(
            ScenarioDto scenario,
            List<BehaviorContext> behaviorContexts,
            List<BehaviorAnalysisResult> analysisResults) {
        if (behaviorContexts.size() != analysisResults.size()) {
            throw new IllegalArgumentException("행동 조건과 분석 결과의 개수가 일치해야 합니다.");
        }

        EnumMap<BehaviorRuleCode, ScoreDelta> scoreSums =
                new EnumMap<>(BehaviorRuleCode.class);
        EnumMap<BehaviorRuleCode, Integer> applicationCounts =
                new EnumMap<>(BehaviorRuleCode.class);
        EnumMap<BehaviorRuleCode, ScoreDelta> weightedContributions =
                new EnumMap<>(BehaviorRuleCode.class);

        for (BehaviorAnalysisResult analysisResult : analysisResults) {
            for (RuleResult ruleResult : analysisResult.getAppliedRules()) {
                BehaviorRuleCode ruleCode = ruleResult.getRuleCode();
                if (ONE_TIME_GAME_RULE_CODES.contains(ruleCode)) {
                    weightedContributions.merge(
                            ruleCode,
                            ruleResult.getScoreDelta(),
                            ScoreDelta::addScoreDelta
                    );
                    continue;
                }
                scoreSums.merge(
                        ruleCode,
                        ruleResult.getScoreDelta(),
                        ScoreDelta::addScoreDelta
                );
                applicationCounts.merge(ruleCode, 1, Integer::sum);
            }
        }

        Map<BehaviorRuleCode, Integer> opportunityCounts = calculateOpportunityCounts(
                scenario,
                behaviorContexts,
                applicationCounts
        );
        scoreSums.forEach((ruleCode, scoreSum) -> {
            int opportunityCount = opportunityCounts.getOrDefault(
                    ruleCode,
                    applicationCounts.getOrDefault(ruleCode, 0)
            );
            weightedContributions.put(
                    ruleCode,
                    calculateOpportunityWeightedScore(scoreSum, opportunityCount)
            );
        });
        return weightedContributions;
    }

    ScoreDelta calculateOpportunityWeightedScore(
            ScoreDelta accumulatedScore,
            int opportunityCount) {
        if (accumulatedScore == null) {
            throw new IllegalArgumentException("누적 점수는 필수입니다.");
        }
        if (opportunityCount <= 0) {
            throw new IllegalArgumentException("행동 가능 기회 수는 0보다 커야 합니다.");
        }
        return divideScoreDelta(
                accumulatedScore,
                BigDecimal.valueOf(opportunityCount + OPPORTUNITY_CONFIDENCE_K)
        );
    }

    private Map<BehaviorRuleCode, Integer> calculateOpportunityCounts(
            ScenarioDto scenario,
            List<BehaviorContext> behaviorContexts,
            Map<BehaviorRuleCode, Integer> applicationCounts) {
        EnumMap<BehaviorRuleCode, Integer> opportunityCounts =
                new EnumMap<>(BehaviorRuleCode.class);
        int crashOpportunityCount = countMarketStateTicks(scenario, MarketState.CRASH);
        int bullOpportunityCount = countMarketStateTicks(scenario, MarketState.BULL);

        opportunityCounts.put(BehaviorRuleCode.CRASH_BUY, crashOpportunityCount);
        opportunityCounts.put(BehaviorRuleCode.CRASH_FULL_SELL, crashOpportunityCount);
        opportunityCounts.put(BehaviorRuleCode.BULL_BUY, bullOpportunityCount);
        opportunityCounts.put(BehaviorRuleCode.BULL_PROFIT_SELL, bullOpportunityCount);

        applicationCounts.forEach((ruleCode, applicationCount) ->
                opportunityCounts.merge(ruleCode, applicationCount, Math::max));
        return opportunityCounts;
    }

    private int countMarketStateTicks(ScenarioDto scenario, MarketState targetMarketState) {
        return (int) scenario.getTicks().stream()
                .filter(tick -> tick.getTick() >= 0 && tick.getTick() < scenario.getTotalTicks())
                .filter(tick -> marketStateCalculator.calculateMarketState(
                        gamePriceRateCalculator.calculateTickPriceChangeRate(
                                scenario,
                                tick.getTick()
                        ),
                        null
                ) == targetMarketState)
                .count();
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

    int calculateRiskBudgetMaintenanceEpisodeCount(
            ScenarioDto scenario,
            long initialCash,
            long initialStockPrincipal,
            long initialDeposit,
            List<SimulatedGameAction> actions) {
        long currentCash = initialCash;
        long currentStockPrincipal = initialStockPrincipal;
        long currentDeposit = initialDeposit;
        int consecutiveTicks = 0;
        int episodeCount = 0;
        boolean episodeApplied = false;
        Map<Integer, List<SimulatedGameAction>> actionsByTick = groupActionsByTick(actions);

        for (ScenarioTickDto scenarioTick : getDecisionTicks(scenario)) {
            boolean securityTraded = false;
            for (SimulatedGameAction action : actionsByTick.getOrDefault(
                    scenarioTick.getTick(),
                    List.of()
            )) {
                if (action.getAssetType() == BehaviorAssetType.SECURITY
                        && (action.getActionType() == BehaviorActionType.BUY
                        || action.getActionType() == BehaviorActionType.SELL)) {
                    securityTraded = true;
                }
                currentCash = action.getCurrentCash();
                currentStockPrincipal = action.getCurrentStockPrincipal();
                currentDeposit = action.getCurrentDeposit();
            }
            if (securityTraded) {
                consecutiveTicks = 0;
                episodeApplied = false;
                continue;
            }
            BigDecimal cashRatio = assetRatioCalculator.calculateCashRatio(
                    currentCash,
                    currentStockPrincipal,
                    currentDeposit
            );
            BigDecimal stockRatio = calculateAssetRatio(
                    currentStockPrincipal,
                    currentCash,
                    currentStockPrincipal,
                    currentDeposit
            );
            boolean maintainsRiskBudget =
                    stockRatio.compareTo(RISK_BUDGET_STOCK_MINIMUM_RATIO) >= 0
                            && stockRatio.compareTo(RISK_BUDGET_STOCK_MAXIMUM_RATIO) < 0
                            && cashRatio.compareTo(CASH_BUFFER_MINIMUM_RATIO) >= 0
                            && cashRatio.compareTo(RISK_BUDGET_CASH_MAXIMUM_RATIO) < 0;
            if (!maintainsRiskBudget) {
                consecutiveTicks = 0;
                episodeApplied = false;
                continue;
            }
            consecutiveTicks++;
            if (!episodeApplied && consecutiveTicks >= CANDIDATE_MAINTENANCE_TICKS) {
                episodeCount++;
                episodeApplied = true;
            }
        }
        return episodeCount;
    }

    int calculatePassiveHighRiskHoldingEpisodeCount(
            ScenarioDto scenario,
            long initialCash,
            long initialStockPrincipal,
            long initialDeposit,
            List<SimulatedGameAction> actions) {
        long currentCash = initialCash;
        long currentStockPrincipal = initialStockPrincipal;
        long currentDeposit = initialDeposit;
        int episodeCount = 0;
        boolean crashEpisode = false;
        boolean qualifiesAtStart = false;
        boolean tradedDuringEpisode = false;
        Map<Integer, List<SimulatedGameAction>> actionsByTick = groupActionsByTick(actions);

        for (ScenarioTickDto scenarioTick : getDecisionTicks(scenario)) {
            MarketState marketState = marketStateCalculator.calculateMarketState(
                    gamePriceRateCalculator.calculateTickPriceChangeRate(
                            scenario,
                            scenarioTick.getTick()
                    ),
                    null
            );
            if (marketState == MarketState.CRASH && !crashEpisode) {
                crashEpisode = true;
                qualifiesAtStart = hasHighRiskLowLiquidityAllocation(
                        currentCash,
                        currentStockPrincipal,
                        currentDeposit
                );
                tradedDuringEpisode = false;
            } else if (marketState != MarketState.CRASH && crashEpisode) {
                if (qualifiesAtStart && !tradedDuringEpisode) {
                    episodeCount++;
                }
                crashEpisode = false;
            }

            for (SimulatedGameAction action : actionsByTick.getOrDefault(
                    scenarioTick.getTick(),
                    List.of()
            )) {
                if (crashEpisode
                        && action.getAssetType() == BehaviorAssetType.SECURITY
                        && (action.getActionType() == BehaviorActionType.BUY
                        || action.getActionType() == BehaviorActionType.SELL)) {
                    tradedDuringEpisode = true;
                }
                currentCash = action.getCurrentCash();
                currentStockPrincipal = action.getCurrentStockPrincipal();
                currentDeposit = action.getCurrentDeposit();
            }
        }
        if (crashEpisode && qualifiesAtStart && !tradedDuringEpisode) {
            episodeCount++;
        }
        return episodeCount;
    }

    int calculateLiquidityPreservingOpportunityCount(
            List<BehaviorContext> behaviorContexts,
            List<BehaviorAnalysisResult> analysisResults) {
        if (behaviorContexts.size() != analysisResults.size()) {
            throw new IllegalArgumentException("행동 조건과 분석 결과의 개수가 일치해야 합니다.");
        }
        Set<Integer> matchedSellTicks = new HashSet<>();
        int opportunityCount = 0;
        for (int buyIndex = 0; buyIndex < behaviorContexts.size(); buyIndex++) {
            BehaviorContext buyContext = behaviorContexts.get(buyIndex);
            BehaviorEvent buyEvent = buyContext.getCurrentEvent();
            if (!isLiquidityPreservingPlannedBuy(
                    buyContext,
                    buyEvent,
                    analysisResults.get(buyIndex)
            )) {
                continue;
            }
            int lastTick = buyEvent.getGameTick() + OPPORTUNITY_REALIZATION_TICKS;
            for (int sellIndex = buyIndex + 1; sellIndex < behaviorContexts.size(); sellIndex++) {
                BehaviorEvent sellEvent = behaviorContexts.get(sellIndex).getCurrentEvent();
                if (sellEvent == null || sellEvent.getGameTick() == null) {
                    continue;
                }
                if (sellEvent.getGameTick() > lastTick) {
                    break;
                }
                if (isLiquidityPreservingProfitSell(sellEvent)
                        && matchedSellTicks.add(sellEvent.getGameTick())) {
                    opportunityCount++;
                    break;
                }
            }
        }
        return opportunityCount;
    }

    int calculateRiskExposureNoChaseEpisodeCount(
            ScenarioDto scenario,
            long initialCash,
            long initialStockPrincipal,
            long initialDeposit,
            int initialStockQuantity,
            List<SimulatedGameAction> actions) {
        return calculateRiskExposureNoChaseEpisodeCount(
                scenario,
                initialCash,
                initialStockPrincipal,
                initialDeposit,
                initialStockQuantity,
                actions,
                false,
                Set.of()
        );
    }

    int calculateRiskExposureNoChaseEpisodeCount(
            ScenarioDto scenario,
            long initialCash,
            long initialStockPrincipal,
            long initialDeposit,
            int initialStockQuantity,
            List<SimulatedGameAction> actions,
            boolean refinedAllocation,
            Set<Integer> excludedOpportunityTicks) {
        long currentCash = initialCash;
        long currentStockPrincipal = initialStockPrincipal;
        long currentDeposit = initialDeposit;
        int currentStockQuantity = initialStockQuantity;
        int episodeStartQuantity = 0;
        int episodeCount = 0;
        boolean bullEpisode = false;
        boolean qualifiesAtStart = false;
        boolean boughtDuringEpisode = false;
        boolean excludedByOpportunity = false;
        Map<Integer, List<SimulatedGameAction>> actionsByTick = groupActionsByTick(actions);

        for (ScenarioTickDto scenarioTick : getDecisionTicks(scenario)) {
            MarketState marketState = marketStateCalculator.calculateMarketState(
                    gamePriceRateCalculator.calculateTickPriceChangeRate(
                            scenario,
                            scenarioTick.getTick()
                    ),
                    null
            );
            if (marketState == MarketState.BULL && !bullEpisode) {
                bullEpisode = true;
                qualifiesAtStart = refinedAllocation
                        ? hasRefinedNoChaseAllocation(
                                currentCash,
                                currentStockPrincipal,
                                currentDeposit
                        )
                        : calculateAssetRatio(
                                currentStockPrincipal,
                                currentCash,
                                currentStockPrincipal,
                                currentDeposit
                        ).compareTo(RISK_BUDGET_STOCK_MINIMUM_RATIO) >= 0;
                episodeStartQuantity = currentStockQuantity;
                boughtDuringEpisode = false;
                excludedByOpportunity = false;
            } else if (marketState != MarketState.BULL && bullEpisode) {
                if (qualifiesAtStart
                        && !boughtDuringEpisode
                        && !excludedByOpportunity
                        && retainsBullExposure(episodeStartQuantity, currentStockQuantity)) {
                    episodeCount++;
                }
                bullEpisode = false;
            }

            if (bullEpisode && excludedOpportunityTicks.contains(scenarioTick.getTick())) {
                excludedByOpportunity = true;
            }

            for (SimulatedGameAction action : actionsByTick.getOrDefault(
                    scenarioTick.getTick(),
                    List.of()
            )) {
                if (bullEpisode
                        && action.getAssetType() == BehaviorAssetType.SECURITY
                        && action.getActionType() == BehaviorActionType.BUY) {
                    boughtDuringEpisode = true;
                }
                currentCash = action.getCurrentCash();
                currentStockPrincipal = action.getCurrentStockPrincipal();
                currentDeposit = action.getCurrentDeposit();
                currentStockQuantity = action.getCurrentStockQuantity();
            }
        }
        if (bullEpisode
                && qualifiesAtStart
                && !boughtDuringEpisode
                && !excludedByOpportunity
                && retainsBullExposure(episodeStartQuantity, currentStockQuantity)) {
            episodeCount++;
        }
        return episodeCount;
    }

    Set<Integer> calculateLhhLiquidityOpportunityTicks(
            List<BehaviorContext> behaviorContexts,
            List<BehaviorAnalysisResult> analysisResults) {
        if (behaviorContexts.size() != analysisResults.size()) {
            throw new IllegalArgumentException("행동 조건과 분석 결과의 개수가 일치해야 합니다.");
        }
        Set<Integer> opportunityTicks = new HashSet<>();
        for (int index = 0; index < behaviorContexts.size(); index++) {
            BehaviorContext context = behaviorContexts.get(index);
            BehaviorEvent event = context.getCurrentEvent();
            if (event == null || event.getGameTick() == null) {
                continue;
            }
            boolean plannedBuy = isLiquidityPreservingPlannedBuy(
                    context,
                    event,
                    analysisResults.get(index)
            );
            boolean profitSell = isLiquidityPreservingProfitSell(event);
            if ((plannedBuy || profitSell) && hasLhhLiquidityAllocation(event)) {
                opportunityTicks.add(event.getGameTick());
            }
        }
        return opportunityTicks;
    }

    int calculateHhlCompositeEpisodeCount(
            ScenarioDto scenario,
            long initialCash,
            long initialStockPrincipal,
            long initialDeposit,
            int initialStockQuantity,
            List<SimulatedGameAction> actions) {
        long currentCash = initialCash;
        long currentStockPrincipal = initialStockPrincipal;
        long currentDeposit = initialDeposit;
        int currentStockQuantity = initialStockQuantity;
        int consecutiveRiskBudgetTicks = 0;
        int episodeStartQuantity = 0;
        boolean riskBudgetEstablished = false;
        boolean bullEpisode = false;
        boolean qualifiesAtStart = false;
        boolean boughtDuringEpisode = false;
        Map<Integer, List<SimulatedGameAction>> actionsByTick = groupActionsByTick(actions);

        for (ScenarioTickDto scenarioTick : getDecisionTicks(scenario)) {
            MarketState marketState = marketStateCalculator.calculateMarketState(
                    gamePriceRateCalculator.calculateTickPriceChangeRate(
                            scenario,
                            scenarioTick.getTick()
                    ),
                    null
            );
            if (marketState == MarketState.BULL && !bullEpisode) {
                bullEpisode = true;
                qualifiesAtStart = riskBudgetEstablished && hasRiskBudgetAllocation(
                        currentCash,
                        currentStockPrincipal,
                        currentDeposit
                );
                episodeStartQuantity = currentStockQuantity;
                boughtDuringEpisode = false;
            } else if (marketState != MarketState.BULL && bullEpisode) {
                if (qualifiesAtStart
                        && !boughtDuringEpisode
                        && retainsBullExposure(episodeStartQuantity, currentStockQuantity)) {
                    return 1;
                }
                bullEpisode = false;
            }

            boolean securityTraded = false;
            for (SimulatedGameAction action : actionsByTick.getOrDefault(
                    scenarioTick.getTick(),
                    List.of()
            )) {
                if (action.getAssetType() == BehaviorAssetType.SECURITY
                        && (action.getActionType() == BehaviorActionType.BUY
                        || action.getActionType() == BehaviorActionType.SELL)) {
                    securityTraded = true;
                }
                if (bullEpisode
                        && action.getAssetType() == BehaviorAssetType.SECURITY
                        && action.getActionType() == BehaviorActionType.BUY) {
                    boughtDuringEpisode = true;
                }
                currentCash = action.getCurrentCash();
                currentStockPrincipal = action.getCurrentStockPrincipal();
                currentDeposit = action.getCurrentDeposit();
                currentStockQuantity = action.getCurrentStockQuantity();
            }

            if (securityTraded || !hasRiskBudgetAllocation(
                    currentCash,
                    currentStockPrincipal,
                    currentDeposit
            )) {
                consecutiveRiskBudgetTicks = 0;
                riskBudgetEstablished = false;
            } else {
                consecutiveRiskBudgetTicks++;
                riskBudgetEstablished =
                        consecutiveRiskBudgetTicks >= CANDIDATE_MAINTENANCE_TICKS;
            }
        }
        return bullEpisode
                && qualifiesAtStart
                && !boughtDuringEpisode
                && retainsBullExposure(episodeStartQuantity, currentStockQuantity)
                ? 1
                : 0;
    }

    int calculateLhhCompletedOpportunityCount(
            List<BehaviorContext> behaviorContexts,
            List<BehaviorAnalysisResult> analysisResults) {
        if (behaviorContexts.size() != analysisResults.size()) {
            throw new IllegalArgumentException("행동 조건과 분석 결과의 개수가 일치해야 합니다.");
        }
        for (int buyIndex = 0; buyIndex < behaviorContexts.size(); buyIndex++) {
            BehaviorContext buyContext = behaviorContexts.get(buyIndex);
            BehaviorEvent buyEvent = buyContext.getCurrentEvent();
            if (!isLiquidityPreservingPlannedBuy(
                    buyContext,
                    buyEvent,
                    analysisResults.get(buyIndex)
            ) || !hasTargetedLhhAllocation(buyEvent)) {
                continue;
            }
            int lastTick = buyEvent.getGameTick() + OPPORTUNITY_REALIZATION_TICKS;
            for (int sellIndex = buyIndex + 1; sellIndex < behaviorContexts.size(); sellIndex++) {
                BehaviorEvent sellEvent = behaviorContexts.get(sellIndex).getCurrentEvent();
                if (sellEvent == null || sellEvent.getGameTick() == null) {
                    continue;
                }
                if (sellEvent.getGameTick() > lastTick) {
                    break;
                }
                if (isLiquidityPreservingProfitSell(sellEvent)
                        && hasTargetedLhhAllocation(sellEvent)) {
                    return 1;
                }
            }
        }
        return 0;
    }

    private boolean hasRiskBudgetAllocation(
            long cash,
            long stockPrincipal,
            long deposit) {
        BigDecimal cashRatio = assetRatioCalculator.calculateCashRatio(
                cash,
                stockPrincipal,
                deposit
        );
        BigDecimal stockRatio = calculateAssetRatio(
                stockPrincipal,
                cash,
                stockPrincipal,
                deposit
        );
        return stockRatio.compareTo(RISK_BUDGET_STOCK_MINIMUM_RATIO) >= 0
                && stockRatio.compareTo(RISK_BUDGET_STOCK_MAXIMUM_RATIO) < 0
                && cashRatio.compareTo(CASH_BUFFER_MINIMUM_RATIO) >= 0
                && cashRatio.compareTo(RISK_BUDGET_CASH_MAXIMUM_RATIO) < 0;
    }

    private boolean hasTargetedLhhAllocation(BehaviorEvent event) {
        long cash = event.getCurrentCash();
        long stockPrincipal = event.getCurrentStockPrincipal();
        long deposit = event.getCurrentDeposit();
        BigDecimal liquidAssetRatio = calculateAssetRatio(
                Math.addExact(cash, deposit),
                cash,
                stockPrincipal,
                deposit
        );
        BigDecimal stockRatio = calculateAssetRatio(
                stockPrincipal,
                cash,
                stockPrincipal,
                deposit
        );
        return liquidAssetRatio.compareTo(TARGETED_LHH_LIQUID_ASSET_MINIMUM_RATIO) >= 0
                && stockRatio.compareTo(TARGETED_LHH_STOCK_MAXIMUM_RATIO) <= 0;
    }

    private boolean hasRefinedNoChaseAllocation(
            long cash,
            long stockPrincipal,
            long deposit) {
        BigDecimal stockRatio = calculateAssetRatio(
                stockPrincipal,
                cash,
                stockPrincipal,
                deposit
        );
        BigDecimal cashRatio = calculateAssetRatio(cash, cash, stockPrincipal, deposit);
        BigDecimal depositRatio = calculateAssetRatio(deposit, cash, stockPrincipal, deposit);
        return stockRatio.compareTo(REFINED_NO_CHASE_STOCK_MINIMUM_RATIO) >= 0
                && cashRatio.compareTo(REFINED_NO_CHASE_LIQUID_ASSET_MAXIMUM_RATIO) < 0
                && depositRatio.compareTo(REFINED_NO_CHASE_LIQUID_ASSET_MAXIMUM_RATIO) < 0;
    }

    private boolean hasLhhLiquidityAllocation(BehaviorEvent event) {
        long cash = event.getCurrentCash();
        long stockPrincipal = event.getCurrentStockPrincipal();
        long deposit = event.getCurrentDeposit();
        BigDecimal liquidAssetRatio = calculateAssetRatio(
                Math.addExact(cash, deposit),
                cash,
                stockPrincipal,
                deposit
        );
        BigDecimal stockRatio = calculateAssetRatio(
                stockPrincipal,
                cash,
                stockPrincipal,
                deposit
        );
        return liquidAssetRatio.compareTo(LHH_LIQUID_ASSET_MINIMUM_RATIO) >= 0
                && stockRatio.compareTo(LHH_STOCK_MAXIMUM_RATIO) <= 0;
    }

    int calculateHighStockLowCashInactivityEpisodeCount(
            ScenarioDto scenario,
            long initialCash,
            long initialStockPrincipal,
            long initialDeposit,
            List<SimulatedGameAction> actions) {
        long currentCash = initialCash;
        long currentStockPrincipal = initialStockPrincipal;
        long currentDeposit = initialDeposit;
        int consecutiveTicks = 0;
        int episodeCount = 0;
        boolean episodeApplied = false;
        Map<Integer, List<SimulatedGameAction>> actionsByTick = groupActionsByTick(actions);

        for (ScenarioTickDto scenarioTick : getDecisionTicks(scenario)) {
            boolean securityTraded = false;
            for (SimulatedGameAction action : actionsByTick.getOrDefault(
                    scenarioTick.getTick(),
                    List.of()
            )) {
                if (action.getAssetType() == BehaviorAssetType.SECURITY
                        && (action.getActionType() == BehaviorActionType.BUY
                        || action.getActionType() == BehaviorActionType.SELL)) {
                    securityTraded = true;
                }
                currentCash = action.getCurrentCash();
                currentStockPrincipal = action.getCurrentStockPrincipal();
                currentDeposit = action.getCurrentDeposit();
            }
            if (securityTraded || !hasHighRiskLowLiquidityAllocation(
                    currentCash,
                    currentStockPrincipal,
                    currentDeposit
            )) {
                consecutiveTicks = 0;
                episodeApplied = false;
                continue;
            }
            consecutiveTicks++;
            if (!episodeApplied && consecutiveTicks >= HIGH_STOCK_INACTIVITY_TICKS) {
                episodeCount++;
                episodeApplied = true;
            }
        }
        return episodeCount;
    }

    private boolean retainsBullExposure(int startQuantity, int endQuantity) {
        if (startQuantity <= 0 || endQuantity <= 0) {
            return false;
        }
        return BigDecimal.valueOf(endQuantity)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(startQuantity), 4, RoundingMode.HALF_UP)
                .compareTo(BULL_EXPOSURE_RETENTION_RATIO) >= 0;
    }

    private boolean isLiquidityPreservingPlannedBuy(
            BehaviorContext context,
            BehaviorEvent event,
            BehaviorAnalysisResult analysisResult) {
        if (event == null
                || event.getGameTick() == null
                || event.getActionType() != BehaviorActionType.BUY
                || event.getAssetType() != BehaviorAssetType.SECURITY
                || context.getMarketState() != MarketState.NORMAL
                || containsRule(analysisResult, BehaviorRuleCode.LOSS_AVERAGING_BUY)) {
            return false;
        }
        BigDecimal buyRatio = calculateActionAmountRatio(event);
        BigDecimal cashRatio = assetRatioCalculator.calculateCashRatio(
                event.getCurrentCash(),
                event.getCurrentStockPrincipal(),
                event.getCurrentDeposit()
        );
        return buyRatio.compareTo(NORMAL_BUY_MINIMUM_RATIO) >= 0
                && buyRatio.compareTo(NORMAL_BUY_MAXIMUM_RATIO) < 0
                && cashRatio.compareTo(CASH_BUFFER_MINIMUM_RATIO) >= 0;
    }

    private boolean isLiquidityPreservingProfitSell(BehaviorEvent event) {
        if (event.getActionType() != BehaviorActionType.SELL
                || event.getAssetType() != BehaviorAssetType.SECURITY
                || event.getRealizedReturnRate() == null
                || event.getRealizedReturnRate().signum() <= 0
                || event.getCurrentSecurityQuantity() == null
                || event.getCurrentSecurityQuantity() <= 0) {
            return false;
        }
        BigDecimal cashRatio = assetRatioCalculator.calculateCashRatio(
                event.getCurrentCash(),
                event.getCurrentStockPrincipal(),
                event.getCurrentDeposit()
        );
        return cashRatio.compareTo(CASH_BUFFER_MINIMUM_RATIO) >= 0;
    }

    private boolean hasHighRiskLowLiquidityAllocation(
            long cash,
            long stockPrincipal,
            long deposit) {
        BigDecimal cashRatio = assetRatioCalculator.calculateCashRatio(
                cash,
                stockPrincipal,
                deposit
        );
        BigDecimal stockRatio = calculateAssetRatio(
                stockPrincipal,
                cash,
                stockPrincipal,
                deposit
        );
        return stockRatio.compareTo(PASSIVE_HIGH_RISK_STOCK_MINIMUM_RATIO) >= 0
                && cashRatio.compareTo(PASSIVE_HIGH_RISK_CASH_MAXIMUM_RATIO) < 0;
    }

    private BigDecimal calculateAssetRatio(
            long amount,
            long cash,
            long stockPrincipal,
            long deposit) {
        long totalPrincipal = Math.addExact(Math.addExact(cash, stockPrincipal), deposit);
        if (amount <= 0 || totalPrincipal <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalPrincipal), 4, RoundingMode.HALF_UP);
    }

    private Map<Integer, List<SimulatedGameAction>> groupActionsByTick(
            List<SimulatedGameAction> actions) {
        return actions.stream()
                .filter(action -> action.getActionType() != BehaviorActionType.MATURITY)
                .collect(java.util.stream.Collectors.groupingBy(
                        SimulatedGameAction::getGameTick,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
    }

    private List<ScenarioTickDto> getDecisionTicks(ScenarioDto scenario) {
        return scenario.getTicks().stream()
                .filter(tick -> tick.getTick() >= 0 && tick.getTick() < scenario.getTotalTicks())
                .sorted(java.util.Comparator.comparingInt(ScenarioTickDto::getTick))
                .toList();
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
