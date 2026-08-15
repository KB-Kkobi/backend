package org.kkobi.assessment.simulation;

import lombok.Builder;
import lombok.Getter;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.assessment.domain.ScoreDelta;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class GameBehaviorSimulationResult {

    private static final BigDecimal TOTAL_RATIO = BigDecimal.valueOf(100);
    private static final BigDecimal MINIMUM_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAXIMUM_SCORE = BigDecimal.valueOf(100);
    private static final BigDecimal PERSONA_BOUNDARY_SCORE = BigDecimal.valueOf(50);
    private static final int TOTAL_GAME_TICKS = 52;

    private final long simulationUserId;
    private final BigDecimal initialCashRatio;
    private final BigDecimal initialStockRatio;
    private final BigDecimal initialDepositRatio;
    private final int buyCount;
    private final int sellCount;
    private final int noActionTickCount;
    private final long totalBuyAmount;
    private final long totalSellAmount;
    private final int fullSellCount;
    private final int crashBuyCount;
    private final int crashFullSellCount;
    private final int bullBuyCount;
    private final int bullProfitSellCount;
    private final int lossAveragingBuyCount;
    private final int lossCutSellCount;
    private final int crashHoldingEpisodeCount;
    private final boolean depositCancelled;
    private final boolean depositMatured;
    private final boolean boughtStockAfterDepositCancel;
    private final int maximumConsecutiveBuyCount;
    private final int maximumConsecutiveSellCount;
    private final int consecutiveActionLevelTwoCount;
    private final int consecutiveActionLevelThreeOrMoreCount;
    private final Map<BehaviorRuleCode, Integer> ruleApplicationCounts;
    private final Map<BehaviorRuleCode, ScoreDelta> ruleScoreContributions;
    private final BigDecimal finalRtScore;
    private final BigDecimal finalLhScore;
    private final BigDecimal finalRpScore;
    private final PersonaType personaType;

    @Builder
    public GameBehaviorSimulationResult(
            long simulationUserId,
            BigDecimal initialCashRatio,
            BigDecimal initialStockRatio,
            BigDecimal initialDepositRatio,
            int buyCount,
            int sellCount,
            int noActionTickCount,
            long totalBuyAmount,
            long totalSellAmount,
            int fullSellCount,
            int crashBuyCount,
            int crashFullSellCount,
            int bullBuyCount,
            int bullProfitSellCount,
            int lossAveragingBuyCount,
            int lossCutSellCount,
            int crashHoldingEpisodeCount,
            boolean depositCancelled,
            boolean depositMatured,
            boolean boughtStockAfterDepositCancel,
            int maximumConsecutiveBuyCount,
            int maximumConsecutiveSellCount,
            int consecutiveActionLevelTwoCount,
            int consecutiveActionLevelThreeOrMoreCount,
            Map<BehaviorRuleCode, Integer> ruleApplicationCounts,
            Map<BehaviorRuleCode, ScoreDelta> ruleScoreContributions,
            BigDecimal finalRtScore,
            BigDecimal finalLhScore,
            BigDecimal finalRpScore,
            PersonaType personaType) {
        this.simulationUserId = validateSimulationUserId(simulationUserId);
        this.initialCashRatio = validateRatio(initialCashRatio, "초기 현금 비율");
        this.initialStockRatio = validateRatio(initialStockRatio, "초기 주식 비율");
        this.initialDepositRatio = validateRatio(initialDepositRatio, "초기 예금 비율");
        validateInitialAllocationRatio();
        this.buyCount = validateCount(buyCount, "매수 횟수");
        this.sellCount = validateCount(sellCount, "매도 횟수");
        this.noActionTickCount = validateNoActionTickCount(noActionTickCount);
        this.totalBuyAmount = validateAmount(totalBuyAmount, "총매수 금액");
        this.totalSellAmount = validateAmount(totalSellAmount, "총매도 금액");
        this.fullSellCount = validateCount(fullSellCount, "전량 매도 횟수");
        this.crashBuyCount = validateCount(crashBuyCount, "급락장 매수 횟수");
        this.crashFullSellCount = validateCount(crashFullSellCount, "급락장 전량 매도 횟수");
        this.bullBuyCount = validateCount(bullBuyCount, "급등장 매수 횟수");
        this.bullProfitSellCount = validateCount(bullProfitSellCount, "급등장 익절 횟수");
        this.lossAveragingBuyCount = validateCount(lossAveragingBuyCount, "손실 구간 추가 매수 횟수");
        this.lossCutSellCount = validateCount(lossCutSellCount, "손절 횟수");
        this.crashHoldingEpisodeCount = validateCount(
                crashHoldingEpisodeCount,
                "급락 구간 보유 유지 횟수"
        );
        validateDepositStatus(depositCancelled, depositMatured);
        this.depositCancelled = depositCancelled;
        this.depositMatured = depositMatured;
        this.boughtStockAfterDepositCancel = boughtStockAfterDepositCancel;
        this.maximumConsecutiveBuyCount = validateCount(
                maximumConsecutiveBuyCount,
                "최대 연속 매수 횟수"
        );
        this.maximumConsecutiveSellCount = validateCount(
                maximumConsecutiveSellCount,
                "최대 연속 매도 횟수"
        );
        this.consecutiveActionLevelTwoCount = validateCount(
                consecutiveActionLevelTwoCount,
                "연속 행동 2회 발생 횟수"
        );
        this.consecutiveActionLevelThreeOrMoreCount = validateCount(
                consecutiveActionLevelThreeOrMoreCount,
                "연속 행동 3회 이상 발생 횟수"
        );
        this.ruleApplicationCounts = copyRuleApplicationCounts(ruleApplicationCounts);
        this.ruleScoreContributions = copyRuleScoreContributions(ruleScoreContributions);
        this.finalRtScore = validateScore(finalRtScore, "최종 RT 점수");
        this.finalLhScore = validateScore(finalLhScore, "최종 LH 점수");
        this.finalRpScore = validateScore(finalRpScore, "최종 RP 점수");
        this.personaType = Objects.requireNonNull(personaType, "최종 성향은 필수입니다.");
    }

    public int getTradeCount() {
        return buyCount + sellCount;
    }

    public BigDecimal getRtBoundaryDistance() {
        return finalRtScore.subtract(PERSONA_BOUNDARY_SCORE).abs();
    }

    public BigDecimal getLhBoundaryDistance() {
        return finalLhScore.subtract(PERSONA_BOUNDARY_SCORE).abs();
    }

    public BigDecimal getRpBoundaryDistance() {
        return finalRpScore.subtract(PERSONA_BOUNDARY_SCORE).abs();
    }

    public boolean isNearBoundary(BigDecimal boundaryRange) {
        if (boundaryRange == null || boundaryRange.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("경계 범위는 0 이상이어야 합니다.");
        }
        return getRtBoundaryDistance().compareTo(boundaryRange) <= 0
                || getLhBoundaryDistance().compareTo(boundaryRange) <= 0
                || getRpBoundaryDistance().compareTo(boundaryRange) <= 0;
    }

    private long validateSimulationUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("시뮬레이션 사용자 ID는 0보다 커야 합니다.");
        }
        return userId;
    }

    private BigDecimal validateRatio(BigDecimal ratio, String ratioName) {
        Objects.requireNonNull(ratio, ratioName + "은 필수입니다.");
        if (ratio.compareTo(BigDecimal.ZERO) < 0 || ratio.compareTo(TOTAL_RATIO) > 0) {
            throw new IllegalArgumentException(ratioName + "은 0 이상 100 이하여야 합니다.");
        }
        return ratio;
    }

    private void validateInitialAllocationRatio() {
        BigDecimal allocationRatio = initialCashRatio
                .add(initialStockRatio)
                .add(initialDepositRatio);
        if (allocationRatio.compareTo(TOTAL_RATIO) != 0) {
            throw new IllegalArgumentException("초기 자산 배분 비율의 합은 100이어야 합니다.");
        }
    }

    private int validateCount(int count, String countName) {
        if (count < 0) {
            throw new IllegalArgumentException(countName + "는 0 이상이어야 합니다.");
        }
        return count;
    }

    private int validateNoActionTickCount(int count) {
        if (count < 0 || count > TOTAL_GAME_TICKS) {
            throw new IllegalArgumentException("무행동 Tick 수는 0 이상 52 이하여야 합니다.");
        }
        return count;
    }

    private long validateAmount(long amount, String amountName) {
        if (amount < 0) {
            throw new IllegalArgumentException(amountName + "은 0 이상이어야 합니다.");
        }
        return amount;
    }

    private void validateDepositStatus(boolean cancelled, boolean matured) {
        if (cancelled && matured) {
            throw new IllegalArgumentException("예금 해지와 만기 유지는 동시에 발생할 수 없습니다.");
        }
    }

    private Map<BehaviorRuleCode, Integer> copyRuleApplicationCounts(
            Map<BehaviorRuleCode, Integer> applicationCounts) {
        if (applicationCounts == null || applicationCounts.isEmpty()) {
            return Map.of();
        }

        EnumMap<BehaviorRuleCode, Integer> copiedCounts = new EnumMap<>(BehaviorRuleCode.class);
        applicationCounts.forEach((ruleCode, count) -> {
            Objects.requireNonNull(ruleCode, "행동 규칙 코드는 필수입니다.");
            copiedCounts.put(ruleCode, validateCount(count, "행동 규칙 적용 횟수"));
        });
        return Map.copyOf(copiedCounts);
    }

    private Map<BehaviorRuleCode, ScoreDelta> copyRuleScoreContributions(
            Map<BehaviorRuleCode, ScoreDelta> scoreContributions) {
        if (scoreContributions == null || scoreContributions.isEmpty()) {
            return Map.of();
        }

        EnumMap<BehaviorRuleCode, ScoreDelta> copiedContributions =
                new EnumMap<>(BehaviorRuleCode.class);
        scoreContributions.forEach((ruleCode, scoreDelta) -> {
            Objects.requireNonNull(ruleCode, "행동 규칙 코드는 필수입니다.");
            copiedContributions.put(
                    ruleCode,
                    Objects.requireNonNull(scoreDelta, "행동 규칙 점수 기여도는 필수입니다.")
            );
        });
        return Map.copyOf(copiedContributions);
    }

    private BigDecimal validateScore(BigDecimal score, String scoreName) {
        Objects.requireNonNull(score, scoreName + "는 필수입니다.");
        if (score.compareTo(MINIMUM_SCORE) < 0 || score.compareTo(MAXIMUM_SCORE) > 0) {
            throw new IllegalArgumentException(scoreName + "는 0 이상 100 이하여야 합니다.");
        }
        return score;
    }
}
