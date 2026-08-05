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
import org.kkobi.game.calculator.GamePriceRateCalculator;
import org.kkobi.game.calculator.GameSecurityReturnCalculator;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.dto.GameBehaviorRequest;
import org.kkobi.game.dto.ScenarioDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameActionService {

    private static final LocalDateTime GAME_START_AT = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final int DAYS_PER_TICK = 7;
    private static final Long GAME_SECURITY_ID = 1L;

    private final ActionLogService actionLogService;
    private final ScenarioService scenarioService;
    private final GamePriceRateCalculator gamePriceRateCalculator;
    private final GameSecurityReturnCalculator gameSecurityReturnCalculator;
    private final BehaviorContextFactory behaviorContextFactory;
    private final BehaviorRuleEngine behaviorRuleEngine;

    @Transactional
    public Long saveGameActionLog(GameBehaviorRequest request) {
        validateGameBehavior(request);
        ScenarioDto scenario = scenarioService.getScenario(request.getScenarioId());
        validateGameTick(request, scenario);
        List<ActionLogDto> actionLogs = actionLogService.getActionLogsByUserId(request.getUserId());
        validateInitialAllocation(request, actionLogs);
        BehaviorEvent currentEvent = createBehaviorEvent(request, scenario, actionLogs);
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
        if (request.getScenarioId() == null || request.getScenarioId().isBlank()) {
            throw new IllegalArgumentException("scenarioId는 필수입니다.");
        }
        if (request.getTick() == null || request.getTick() < 0) {
            throw new IllegalArgumentException("tick은 0 이상이어야 합니다.");
        }
        if (request.getActionType() == null || request.getAssetType() == null) {
            throw new IllegalArgumentException("actionType과 assetType은 필수입니다.");
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

    private void validateGameTick(GameBehaviorRequest request, ScenarioDto scenario) {
        if (request.getTick() > scenario.getTotalTicks()) {
            throw new IllegalArgumentException(
                    "tick은 0부터 " + scenario.getTotalTicks() + " 사이여야 합니다."
            );
        }
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

    private BehaviorEvent createBehaviorEvent(
            GameBehaviorRequest request,
            ScenarioDto scenario,
            List<ActionLogDto> actionLogs) {
        BehaviorEvent event = new BehaviorEvent();
        event.setUserId(request.getUserId());
        event.setGameTick(request.getTick());
        event.setActionType(BehaviorActionType.getBehaviorActionType(request.getActionType()));
        event.setAssetType(BehaviorAssetType.getBehaviorAssetType(request.getAssetType()));
        updateGameSecurityId(event);
        event.setActionAmount(request.getActionAmount());
        event.setCurrentCash(request.getCurrentCash());
        event.setCurrentStockPrincipal(request.getCurrentStock());
        event.setCurrentDeposit(request.getCurrentDeposit());
        event.setCurrentPriceChangeRate(gamePriceRateCalculator.calculateTickPriceChangeRate(
                scenario,
                request.getTick()
        ));
        event.setDailyPriceRangeRate(request.getDailyPriceRangeRate());
        updateGameSecurityReturnRate(event, scenario, request.getTick(), actionLogs);
        event.setTradedAt(calculateGameActionAt(request.getTick()));
        return event;
    }

    private void updateGameSecurityReturnRate(
            BehaviorEvent event,
            ScenarioDto scenario,
            int gameTick,
            List<ActionLogDto> actionLogs) {
        if (event.getAssetType() != BehaviorAssetType.SECURITY) {
            return;
        }

        BigDecimal currentReturnRate = gameSecurityReturnCalculator.calculateCurrentReturnRate(
                scenario,
                gameTick,
                actionLogs
        );
        if (event.getActionType() == BehaviorActionType.BUY) {
            event.setPositionReturnRate(currentReturnRate);
        } else if (event.getActionType() == BehaviorActionType.SELL) {
            event.setRealizedReturnRate(currentReturnRate);
        }
    }

    private BehaviorEvent createBehaviorEvent(ActionLogDto actionLog) {
        BehaviorEvent event = new BehaviorEvent();
        event.setUserId(actionLog.getUserId());
        event.setGameTick(actionLog.getGameTick());
        event.setActionSequence(actionLog.getActionLogId());
        event.setActionType(BehaviorActionType.getBehaviorActionType(actionLog.getActionType()));
        event.setAssetType(BehaviorAssetType.getBehaviorAssetType(actionLog.getAssetType()));
        updateGameSecurityId(event);
        event.setActionAmount(actionLog.getActionAmount());
        event.setCurrentCash(actionLog.getCurrentCash());
        event.setCurrentStockPrincipal(actionLog.getCurrentStock());
        event.setCurrentDeposit(actionLog.getCurrentDeposit());
        event.setMarketState(MarketState.getMarketState(actionLog.getMarketState()));
        event.setTradedAt(calculateGameActionAt(actionLog.getGameTick()));
        return event;
    }

    private LocalDateTime calculateGameActionAt(Integer gameTick) {
        return GAME_START_AT.plusDays((long) gameTick * DAYS_PER_TICK);
    }

    private void updateGameSecurityId(BehaviorEvent event) {
        if (event.getAssetType() == BehaviorAssetType.SECURITY) {
            event.setSecurityId(GAME_SECURITY_ID);
        }
    }

    private ActionLogDto createActionLog(
            GameBehaviorRequest request,
            BehaviorContext behaviorContext,
            BehaviorAnalysisResult analysisResult) {
        ActionLogDto actionLog = new ActionLogDto();
        actionLog.setUserId(request.getUserId());
        actionLog.setGameTick(request.getTick());
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
        return marketState.name();
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
