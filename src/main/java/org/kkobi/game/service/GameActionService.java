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
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.dto.GameBehaviorRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameActionService {

    private final ActionLogService actionLogService;
    private final BehaviorContextFactory behaviorContextFactory;
    private final BehaviorRuleEngine behaviorRuleEngine;

    @Transactional
    public Long saveGameActionLog(GameBehaviorRequest request) {
        validateGameBehavior(request);
        BehaviorEvent currentEvent = createBehaviorEvent(request);
        List<ActionLogDto> actionLogs = actionLogService.getActionLogsByUserId(request.getUserId());
        validateInitialAllocation(request, actionLogs);
        List<BehaviorEvent> previousEvents = actionLogs
                .stream()
                .map(this::createBehaviorEvent)
                .toList();
        BehaviorContext behaviorContext = behaviorContextFactory.createBehaviorContext(
                currentEvent,
                previousEvents
        );
        BehaviorAnalysisResult analysisResult = behaviorRuleEngine.calculateBehaviorAnalysis(behaviorContext);
        ActionLogDto actionLog = createActionLog(request, behaviorContext, analysisResult);

        return actionLogService.saveActionLog(actionLog);
    }

    private void validateGameBehavior(GameBehaviorRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("게임 행동 요청은 필수입니다.");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (request.getGameMonth() == null
                || request.getGameMonth() < 1
                || request.getGameMonth() > 12) {
            throw new IllegalArgumentException("gameMonth는 1부터 12 사이여야 합니다.");
        }
        if (request.getActionType() == null || request.getAssetType() == null) {
            throw new IllegalArgumentException("actionType과 assetType은 필수입니다.");
        }
        if (request.getActedAt() == null) {
            throw new IllegalArgumentException("actedAt은 필수입니다.");
        }

        BehaviorActionType actionType = BehaviorActionType.getBehaviorActionType(request.getActionType());
        BehaviorAssetType assetType = BehaviorAssetType.getBehaviorAssetType(request.getAssetType());
        if (actionType != BehaviorActionType.INITIAL_ALLOCATION
                && actionType != BehaviorActionType.BUY
                && actionType != BehaviorActionType.SELL
                && actionType != BehaviorActionType.CANCEL_PRODUCT) {
            throw new IllegalArgumentException("성향 파악 게임에서 지원하지 않는 행동입니다: " + request.getActionType());
        }

        validateActionAssetType(actionType, assetType);
        validateAssetSnapshot(request);
    }

    private void validateActionAssetType(
            BehaviorActionType actionType,
            BehaviorAssetType assetType) {
        if (actionType == BehaviorActionType.INITIAL_ALLOCATION
                && assetType != BehaviorAssetType.ALL) {
            throw new IllegalArgumentException("초기 자산 배분의 assetType은 ALL이어야 합니다.");
        }
        if ((actionType == BehaviorActionType.BUY || actionType == BehaviorActionType.SELL)
                && assetType != BehaviorAssetType.SECURITY) {
            throw new IllegalArgumentException("매수와 매도의 assetType은 STOCK이어야 합니다.");
        }
        if (actionType == BehaviorActionType.CANCEL_PRODUCT
                && assetType != BehaviorAssetType.PRODUCT) {
            throw new IllegalArgumentException("예금 해지의 assetType은 DEPOSIT이어야 합니다.");
        }
    }

    private void validateAssetSnapshot(GameBehaviorRequest request) {
        if (request.getCurrentCash() == null
                || request.getCurrentStock() == null
                || request.getCurrentDeposit() == null) {
            throw new IllegalArgumentException("행동 후 자산 금액은 모두 필수입니다.");
        }
        if (request.getCurrentCash() < 0
                || request.getCurrentStock() < 0
                || request.getCurrentDeposit() < 0) {
            throw new IllegalArgumentException("행동 후 자산 금액은 0 이상이어야 합니다.");
        }
    }

    private void validateInitialAllocation(
            GameBehaviorRequest request,
            List<ActionLogDto> actionLogs) {
        if (!"INITIAL_ALLOCATION".equals(request.getActionType())) {
            return;
        }

        boolean existsInitialAllocation = actionLogs.stream()
                .anyMatch(actionLog -> "INITIAL_ALLOCATION".equals(actionLog.getActionType()));
        if (existsInitialAllocation) {
            throw new IllegalStateException("초기 자산 배분은 한 번만 기록할 수 있습니다.");
        }
    }

    private BehaviorEvent createBehaviorEvent(GameBehaviorRequest request) {
        BehaviorEvent event = new BehaviorEvent();
        event.setUserId(request.getUserId());
        event.setActionType(BehaviorActionType.getBehaviorActionType(request.getActionType()));
        event.setAssetType(BehaviorAssetType.getBehaviorAssetType(request.getAssetType()));
        event.setActionAmount(request.getActionAmount());
        event.setCurrentCash(request.getCurrentCash());
        event.setCurrentStockPrincipal(request.getCurrentStock());
        event.setCurrentDeposit(request.getCurrentDeposit());
        event.setCurrentPriceChangeRate(request.getCurrentPriceChangeRate());
        event.setDailyPriceRangeRate(request.getDailyPriceRangeRate());
        event.setRealizedReturnRate(request.getRealizedReturnRate());
        event.setPositionReturnRate(request.getPositionReturnRate());
        event.setTradedAt(request.getActedAt());
        return event;
    }

    private BehaviorEvent createBehaviorEvent(ActionLogDto actionLog) {
        BehaviorEvent event = new BehaviorEvent();
        event.setUserId(actionLog.getUserId());
        event.setActionType(BehaviorActionType.getBehaviorActionType(actionLog.getActionType()));
        event.setAssetType(BehaviorAssetType.getBehaviorAssetType(actionLog.getAssetType()));
        event.setActionAmount(actionLog.getActionAmount());
        event.setCurrentCash(actionLog.getCurrentCash());
        event.setCurrentStockPrincipal(actionLog.getCurrentStock());
        event.setCurrentDeposit(actionLog.getCurrentDeposit());
        event.setMarketState(MarketState.getMarketState(actionLog.getMarketState()));
        if (actionLog.getCreatedAt() != null) {
            event.setTradedAt(actionLog.getCreatedAt().toLocalDateTime());
        }
        return event;
    }

    private ActionLogDto createActionLog(
            GameBehaviorRequest request,
            BehaviorContext behaviorContext,
            BehaviorAnalysisResult analysisResult) {
        ActionLogDto actionLog = new ActionLogDto();
        actionLog.setUserId(request.getUserId());
        actionLog.setGameMonth(request.getGameMonth());
        actionLog.setActionType(getActionLogActionType(behaviorContext.getCurrentEvent().getActionType()));
        actionLog.setAssetType(getActionLogAssetType(behaviorContext.getCurrentEvent().getAssetType()));
        actionLog.setActionAmount(request.getActionAmount());
        actionLog.setMarketState(getActionLogMarketState(behaviorContext.getMarketState()));
        actionLog.setDepositStatus(getDepositStatus(behaviorContext.getCurrentEvent().getActionType()));
        actionLog.setCurrentCash(request.getCurrentCash());
        actionLog.setCurrentStock(request.getCurrentStock());
        actionLog.setCurrentDeposit(request.getCurrentDeposit());
        actionLog.setRtScoreDelta(analysisResult.getTotalScoreDelta().getRtDelta());
        actionLog.setLhScoreDelta(analysisResult.getTotalScoreDelta().getLhDelta());
        actionLog.setRpScoreDelta(analysisResult.getTotalScoreDelta().getRpDelta());
        return actionLog;
    }

    private String getActionLogActionType(BehaviorActionType actionType) {
        return actionType == BehaviorActionType.CANCEL_PRODUCT
                ? "DEPOSIT_CANCEL"
                : actionType.name();
    }

    private String getActionLogAssetType(BehaviorAssetType assetType) {
        return switch (assetType) {
            case SECURITY -> "STOCK";
            case PRODUCT -> "DEPOSIT";
            default -> assetType.name();
        };
    }

    private String getActionLogMarketState(MarketState marketState) {
        return marketState == MarketState.NORMAL ? "SIDEWAYS" : marketState.name();
    }

    private String getDepositStatus(BehaviorActionType actionType) {
        if (actionType == BehaviorActionType.CANCEL_PRODUCT) {
            return "CANCELLED";
        }
        if (actionType == BehaviorActionType.MATURITY) {
            return "MATURED";
        }
        return "NONE";
    }
}
