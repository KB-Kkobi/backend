package org.kkobi.game.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.calculator.AssetRatioCalculator;
import org.kkobi.assessment.calculator.BehaviorContextFactory;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.MarketStateCalculator;
import org.kkobi.assessment.calculator.PersonaClassifier;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.dto.AssessmentResultResponseDto;
import org.kkobi.assessment.mapper.AssessmentMapper;
import org.kkobi.assessment.service.AssessmentResultService;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.dto.GameStartRequest;
import org.kkobi.game.dto.GameStartResponse;
import org.kkobi.game.mapper.ActionLogMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameStartServiceTest {

    @Test
    @DisplayName("초기 자산 비율을 금액으로 계산하고 초기 행동 로그를 저장한다.")
    void startGameSavesInitialAllocation() {
        InMemoryActionLogMapper actionLogMapper = new InMemoryActionLogMapper();
        GameStartService gameStartService = createGameStartService(actionLogMapper, null);

        GameStartResponse response = gameStartService.startGame(
                1L,
                createGameStartRequest("20", "50", "30")
        );

        assertEquals(10_000_000L, response.getSeedMoney());
        assertEquals(2_000_000L, response.getCashAmount());
        assertEquals(5_000_000L, response.getStockAmount());
        assertEquals(3_000_000L, response.getDepositAmount());
        assertEquals(0, response.getCurrentTick());
        assertEquals(52, response.getTotalTick());
        assertEquals("ACTIVE", response.getDepositStatus());

        ActionLogDto actionLog = actionLogMapper.getActionLogsByUserId(1L).get(0);
        assertEquals("INITIAL_ALLOCATION", actionLog.getActionType());
        assertEquals("ALL", actionLog.getAssetType());
        assertEquals(10_000_000L, actionLog.getActionAmount());
        assertEquals("NORMAL", actionLog.getMarketState());
        assertEquals("ACTIVE", actionLog.getDepositStatus());
    }

    @Test
    @DisplayName("초기 자산 비율의 합이 100이 아니면 게임을 시작하지 않는다.")
    void startGameRejectsInvalidRatioSum() {
        GameStartService gameStartService = createGameStartService(
                new InMemoryActionLogMapper(),
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> gameStartService.startGame(
                        1L,
                        createGameStartRequest("20", "50", "20")
                )
        );
    }

    @Test
    @DisplayName("성향 결과가 있는 사용자는 게임을 다시 시작할 수 없다.")
    void startGameRejectsCompletedUser() {
        GameStartService gameStartService = createGameStartService(
                new InMemoryActionLogMapper(),
                AssessmentScore.createInitialScore()
        );

        assertThrows(
                IllegalStateException.class,
                () -> gameStartService.startGame(
                        1L,
                        createGameStartRequest("20", "50", "30")
                )
        );
    }

    @Test
    @DisplayName("다른 성향 결과만 있는 사용자는 게임을 시작할 수 있다.")
    void startGameAllowsUserWithoutGameCompletionResult() {
        InMemoryActionLogMapper actionLogMapper = new InMemoryActionLogMapper();
        GameStartService gameStartService = createGameStartService(
                actionLogMapper,
                AssessmentScore.createInitialScore(),
                false
        );

        GameStartResponse response = gameStartService.startGame(
                1L,
                createGameStartRequest("20", "50", "30")
        );

        assertEquals(10_000_000L, response.getSeedMoney());
        assertEquals(1, actionLogMapper.getActionLogsByUserId(1L).size());
    }

    @Test
    @DisplayName("미완료 사용자가 다시 시작하면 이전 행동 로그를 초기화한다.")
    void startGameDeletesPreviousIncompleteActionLogs() {
        InMemoryActionLogMapper actionLogMapper = new InMemoryActionLogMapper();
        ActionLogDto previousActionLog = new ActionLogDto();
        previousActionLog.setUserId(1L);
        previousActionLog.setActionType("BUY");
        actionLogMapper.saveActionLog(previousActionLog);
        GameStartService gameStartService = createGameStartService(actionLogMapper, null);

        gameStartService.startGame(
                1L,
                createGameStartRequest("20", "50", "30")
        );

        List<ActionLogDto> actionLogs = actionLogMapper.getActionLogsByUserId(1L);
        assertEquals(1, actionLogs.size());
        assertEquals("INITIAL_ALLOCATION", actionLogs.get(0).getActionType());
    }

    private GameStartService createGameStartService(
            InMemoryActionLogMapper actionLogMapper,
            AssessmentScore latestAssessmentScore) {
        return createGameStartService(
                actionLogMapper,
                latestAssessmentScore,
                latestAssessmentScore != null
        );
    }

    private GameStartService createGameStartService(
            InMemoryActionLogMapper actionLogMapper,
            AssessmentScore latestAssessmentScore,
            boolean completedGame) {
        actionLogMapper.setCompletedGame(completedGame);
        AssessmentResultService assessmentResultService = new AssessmentResultService(
                createAssessmentMapper(latestAssessmentScore),
                new PersonaClassifier()
        );
        return new GameStartService(
                new ActionLogService(actionLogMapper),
                assessmentResultService,
                new BehaviorContextFactory(
                        new AssetRatioCalculator(),
                        new MarketStateCalculator()
                ),
                new BehaviorRuleEngine()
        );
    }

    private AssessmentMapper createAssessmentMapper(AssessmentScore latestAssessmentScore) {
        return new AssessmentMapper() {
            @Override
            public AssessmentScore getLatestAssessmentScore(Long userId) {
                return latestAssessmentScore;
            }

            @Override
            public org.kkobi.assessment.domain.AssessmentResultDetails
                    getLatestAssessmentResultDetails(Long userId) {
                return null;
            }

            @Override
            public AssessmentResultResponseDto getLatestAssessmentResult(Long userId) {
                return null;
            }

            @Override
            public Long getPersonaIdByAxisCode(String axisCode) {
                return 1L;
            }

            @Override
            public int saveAssessmentResult(
                    Long userId,
                    Long personaId,
                    AssessmentScore assessmentScore) {
                return 1;
            }
        };
    }

    private GameStartRequest createGameStartRequest(
            String cashRatio,
            String stockRatio,
            String depositRatio) {
        GameStartRequest request = new GameStartRequest();
        request.setCashRatio(new BigDecimal(cashRatio));
        request.setStockRatio(new BigDecimal(stockRatio));
        request.setDepositRatio(new BigDecimal(depositRatio));
        return request;
    }

    private static class InMemoryActionLogMapper implements ActionLogMapper {

        private final List<ActionLogDto> actionLogs = new ArrayList<>();
        private long nextActionLogId = 1L;
        private boolean completedGame;

        @Override
        public int saveActionLog(ActionLogDto actionLog) {
            actionLog.setActionLogId(nextActionLogId++);
            actionLogs.add(actionLog);
            return 1;
        }

        @Override
        public List<ActionLogDto> getActionLogsByUserId(Long userId) {
            return actionLogs.stream()
                    .filter(actionLog -> userId.equals(actionLog.getUserId()))
                    .toList();
        }

        @Override
        public boolean existsCompletedGame(Long userId) {
            return completedGame;
        }

        @Override
        public Long lockUserById(Long userId) {
            return userId;
        }

        @Override
        public int deleteActionLogsByUserId(Long userId) {
            int previousSize = actionLogs.size();
            actionLogs.removeIf(actionLog -> userId.equals(actionLog.getUserId()));
            return previousSize - actionLogs.size();
        }

        private void setCompletedGame(boolean completedGame) {
            this.completedGame = completedGame;
        }
    }
}
