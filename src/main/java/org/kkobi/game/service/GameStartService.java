package org.kkobi.game.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.calculator.BehaviorContextFactory;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.kkobi.assessment.enums.MarketState;
import org.kkobi.assessment.service.AssessmentResultService;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.dto.GameStartRequest;
import org.kkobi.game.dto.GameStartResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameStartService {

    private static final long SEED_MONEY = 10_000_000L;
    private static final int START_TICK = 0;
    private static final int TOTAL_TICK = 52;
    private static final BigDecimal TOTAL_RATIO = BigDecimal.valueOf(100);

    private final ActionLogService actionLogService;
    private final AssessmentResultService assessmentResultService;
    private final BehaviorContextFactory behaviorContextFactory;
    private final BehaviorRuleEngine behaviorRuleEngine;

    @Transactional
    public GameStartResponse startGame(Long userId, GameStartRequest request) {
        validateGameStart(userId, request);
        actionLogService.lockGameUser(userId);
        validateCompletedGame(userId);

        long stockAmount = calculateAssetAmount(request.getStockRatio());
        long depositAmount = calculateAssetAmount(request.getDepositRatio());
        long cashAmount = SEED_MONEY - stockAmount - depositAmount;

        actionLogService.deleteActionLogsByUserId(userId);
        saveInitialAllocation(userId, cashAmount, stockAmount, depositAmount);

        return new GameStartResponse(
                SEED_MONEY,
                cashAmount,
                stockAmount,
                depositAmount,
                START_TICK,
                TOTAL_TICK,
                depositAmount > 0 ? "ACTIVE" : "NONE"
        );
    }

    private void validateGameStart(Long userId, GameStartRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (request == null
                || request.getCashRatio() == null
                || request.getStockRatio() == null
                || request.getDepositRatio() == null) {
            throw new IllegalArgumentException("초기 자산 비율은 모두 필수입니다.");
        }
        validateRatioRange(request.getCashRatio());
        validateRatioRange(request.getStockRatio());
        validateRatioRange(request.getDepositRatio());

        BigDecimal ratioSum = request.getCashRatio()
                .add(request.getStockRatio())
                .add(request.getDepositRatio());
        if (ratioSum.compareTo(TOTAL_RATIO) != 0) {
            throw new IllegalArgumentException("초기 자산 비율의 합은 100이어야 합니다.");
        }
    }

    private void validateRatioRange(BigDecimal ratio) {
        if (ratio.compareTo(BigDecimal.ZERO) < 0
                || ratio.compareTo(TOTAL_RATIO) > 0) {
            throw new IllegalArgumentException("초기 자산 비율은 0 이상 100 이하여야 합니다.");
        }
    }

    private void validateCompletedGame(Long userId) {
        if (actionLogService.existsCompletedGame(userId)) {
            throw new IllegalStateException("이미 완료한 게임입니다.");
        }
    }

    private long calculateAssetAmount(BigDecimal ratio) {
        return BigDecimal.valueOf(SEED_MONEY)
                .multiply(ratio)
                .divide(TOTAL_RATIO, 0, RoundingMode.DOWN)
                .longValueExact();
    }

    private void saveInitialAllocation(
            Long userId,
            long cashAmount,
            long stockAmount,
            long depositAmount) {
        BehaviorEvent initialAllocation = createInitialAllocation(
                userId,
                cashAmount,
                stockAmount,
                depositAmount
        );
        BehaviorContext behaviorContext = behaviorContextFactory.createBehaviorContext(
                initialAllocation,
                List.of()
        );
        BehaviorAnalysisResult analysisResult = behaviorRuleEngine.calculateGameBehaviorAnalysis(
                behaviorContext
        );

        ActionLogDto actionLog = new ActionLogDto();
        actionLog.setUserId(userId);
        actionLog.setGameTick(START_TICK);
        actionLog.setActionType("INITIAL_ALLOCATION");
        actionLog.setAssetType("ALL");
        actionLog.setActionAmount(SEED_MONEY);
        actionLog.setMarketState(MarketState.NORMAL.name());
        actionLog.setDepositStatus(depositAmount > 0 ? "ACTIVE" : "NONE");
        actionLog.setCurrentCash(cashAmount);
        actionLog.setCurrentStock(stockAmount);
        actionLog.setCurrentDeposit(depositAmount);
        actionLog.setRtScoreDelta(analysisResult.getTotalScoreDelta().getRtDelta());
        actionLog.setLhScoreDelta(analysisResult.getTotalScoreDelta().getLhDelta());
        actionLog.setRpScoreDelta(analysisResult.getTotalScoreDelta().getRpDelta());
        actionLogService.saveActionLog(actionLog);
    }

    private BehaviorEvent createInitialAllocation(
            Long userId,
            long cashAmount,
            long stockAmount,
            long depositAmount) {
        BehaviorEvent initialAllocation = new BehaviorEvent();
        initialAllocation.setUserId(userId);
        initialAllocation.setGameTick(START_TICK);
        initialAllocation.setActionType(BehaviorActionType.INITIAL_ALLOCATION);
        initialAllocation.setAssetType(BehaviorAssetType.ALL);
        initialAllocation.setActionAmount(SEED_MONEY);
        initialAllocation.setCurrentCash(cashAmount);
        initialAllocation.setCurrentStockPrincipal(stockAmount);
        initialAllocation.setCurrentDeposit(depositAmount);
        initialAllocation.setMarketState(MarketState.NORMAL);
        return initialAllocation;
    }
}
