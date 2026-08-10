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
import org.kkobi.game.dto.GameActionRequest;
import org.kkobi.game.dto.GameActionResponse;
import org.kkobi.game.dto.GameBehaviorRequest;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameActionService {

    private static final Long GAME_SECURITY_ID = 1L;
    private static final String GAME_SCENARIO_ID = "SC001";

    private final ActionLogService actionLogService;
    private final ScenarioService scenarioService;
    private final GamePriceRateCalculator gamePriceRateCalculator;
    private final GameSecurityReturnCalculator gameSecurityReturnCalculator;
    private final BehaviorContextFactory behaviorContextFactory;
    private final BehaviorRuleEngine behaviorRuleEngine;

    @Transactional
    public GameActionResponse saveGameAction(Long userId, GameActionRequest request) {
        validateStartedGame(userId);
        GameBehaviorRequest gameBehaviorRequest = createGameBehaviorRequest(userId, request);
        ActionLogDto actionLog = saveGameActionLog(gameBehaviorRequest);
        return createGameActionResponse(actionLog);
    }

    @Transactional
    public ActionLogDto saveGameActionLog(GameBehaviorRequest request) {
        validateGameBehavior(request);
        ScenarioDto scenario = scenarioService.getScenario(request.getScenarioId());
        validateGameTick(request, scenario);
        List<ActionLogDto> actionLogs = actionLogService.getActionLogsByUserId(request.getUserId());
        validateInitialAllocation(request, actionLogs);
        BehaviorEvent currentEvent = createBehaviorEvent(request, scenario, actionLogs);
        List<BehaviorEvent> previousEvents = actionLogs
                .stream()
                .map(actionLog -> createBehaviorEvent(actionLog, scenario))
                .toList();
        BehaviorContext behaviorContext = behaviorContextFactory.createBehaviorContext(
                currentEvent,
                previousEvents
        );
        BehaviorAnalysisResult analysisResult = behaviorRuleEngine.calculateBehaviorAnalysis(behaviorContext);
        ActionLogDto actionLog = createActionLog(request, behaviorContext, analysisResult);

        actionLogService.saveActionLog(actionLog);
        return actionLog;
    }

    private void validateStartedGame(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        boolean existsInitialAllocation = actionLogService.getActionLogsByUserId(userId)
                .stream()
                .anyMatch(actionLog -> "INITIAL_ALLOCATION".equals(actionLog.getActionType()));
        if (!existsInitialAllocation) {
            throw new IllegalStateException("게임을 먼저 시작해야 합니다.");
        }
    }

    private GameBehaviorRequest createGameBehaviorRequest(
            Long userId,
            GameActionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("게임 행동 요청은 필수입니다.");
        }
        GameBehaviorRequest gameBehaviorRequest = new GameBehaviorRequest();
        gameBehaviorRequest.setUserId(userId);
        gameBehaviorRequest.setScenarioId(GAME_SCENARIO_ID);
        gameBehaviorRequest.setTick(request.getGameTick());
        gameBehaviorRequest.setActionType(request.getActionType());
        gameBehaviorRequest.setAssetType(request.getAssetType());
        gameBehaviorRequest.setActionAmount(request.getActionAmount());
        gameBehaviorRequest.setCurrentCash(request.getCurrentCash());
        gameBehaviorRequest.setCurrentStock(request.getCurrentStockPrincipal());
        gameBehaviorRequest.setCurrentDeposit(request.getCurrentDeposit());
        return gameBehaviorRequest;
    }

    private GameActionResponse createGameActionResponse(ActionLogDto actionLog) {
        long totalAssetPrincipal = Math.addExact(
                Math.addExact(actionLog.getCurrentCash(), actionLog.getCurrentStock()),
                actionLog.getCurrentDeposit()
        );
        return new GameActionResponse(
                actionLog.getActionLogId(),
                actionLog.getGameTick(),
                actionLog.getActionType(),
                actionLog.getAssetType(),
                actionLog.getActionAmount(),
                actionLog.getCurrentCash(),
                actionLog.getCurrentStock(),
                actionLog.getCurrentDeposit(),
                totalAssetPrincipal,
                actionLog.getMarketState(),
                actionLog.getDepositStatus(),
                actionLog.getRtScoreDelta(),
                actionLog.getLhScoreDelta(),
                actionLog.getRpScoreDelta()
        );
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
        validateActionAmount(actionType, request.getActionAmount());
        validateAssetSnapshot(request);
    }

    private void validateActionAmount(
            BehaviorActionType actionType,
            Long actionAmount) {
        if (actionAmount == null || actionAmount < 0) {
            throw new IllegalArgumentException("행동 금액은 0 이상이어야 합니다.");
        }
        if ((actionType == BehaviorActionType.BUY || actionType == BehaviorActionType.SELL)
                && actionAmount == 0) {
            throw new IllegalArgumentException("매수·매도 금액은 0보다 커야 합니다.");
        }
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
        event.setTradedAt(calculateGameActionAt(scenario, request.getTick()));
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

    private BehaviorEvent createBehaviorEvent(
            ActionLogDto actionLog,
            ScenarioDto scenario) {
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
        event.setTradedAt(calculateGameActionAt(scenario, actionLog.getGameTick()));
        return event;
    }

    private LocalDateTime calculateGameActionAt(
            ScenarioDto scenario,
            Integer gameTick) {
        return scenario.getTicks()
                .stream()
                .filter(scenarioTick -> scenarioTick.getTick() == gameTick)
                .map(ScenarioTickDto::getDate)
                .filter(scenarioDate -> scenarioDate != null && !scenarioDate.isBlank())
                .map(LocalDate::parse)
                .map(LocalDate::atStartOfDay)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "게임 시나리오 tick 날짜를 찾을 수 없습니다: " + gameTick
                ));
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
        actionLog.setDepositStatus(getDepositStatus(
                behaviorContext.getCurrentEvent().getActionType(),
                request.getCurrentDeposit()
        ));
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

    private String getDepositStatus(
            BehaviorActionType actionType,
            Long currentDeposit) {
        if (actionType == BehaviorActionType.CANCEL_PRODUCT) {
            return "CANCELLED";
        }
        if (actionType == BehaviorActionType.MATURITY) {
            return "MATURED";
        }
        return currentDeposit != null && currentDeposit > 0L
                ? "ACTIVE"
                : "NONE";
    }
}
