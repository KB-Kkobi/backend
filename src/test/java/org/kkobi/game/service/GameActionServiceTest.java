package org.kkobi.game.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.config.RootConfig;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.dto.GameBehaviorRequest;
import org.kkobi.game.mapper.ActionLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@Transactional
class GameActionServiceTest {

    private final GameActionService gameActionService;
    private final ActionLogMapper actionLogMapper;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    GameActionServiceTest(
            GameActionService gameActionService,
            ActionLogMapper actionLogMapper,
            DataSource dataSource) {
        this.gameActionService = gameActionService;
        this.actionLogMapper = actionLogMapper;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("같은 tick의 예금 해지 후 증권 매수에 연관 행동 점수를 적용한다.")
    void saveGameActionLogAppliesRelatedActionScoreWithinSameTick() {
        ActionLogDto sameTickBuyLog = saveDepositCancelAndSecurityBuy(1, 1);
        ActionLogDto nextTickBuyLog = saveDepositCancelAndSecurityBuy(1, 2);

        assertEquals("NORMAL", sameTickBuyLog.getMarketState());
        assertEquals(
                0,
                sameTickBuyLog.getRtScoreDelta()
                        .subtract(nextTickBuyLog.getRtScoreDelta())
                        .compareTo(new BigDecimal("5.00"))
        );
        assertEquals(
                0,
                sameTickBuyLog.getLhScoreDelta()
                        .subtract(nextTickBuyLog.getLhScoreDelta())
                        .compareTo(new BigDecimal("-10.00"))
        );
        assertEquals(
                0,
                sameTickBuyLog.getRpScoreDelta()
                        .subtract(nextTickBuyLog.getRpScoreDelta())
                        .compareTo(new BigDecimal("10.00"))
        );
    }

    @Test
    @DisplayName("게임 행동 후 예금 보유 여부에 따라 예금 상태를 저장한다.")
    void saveGameActionLogStoresDepositStatus() {
        Long userId = createUser();
        GameBehaviorRequest initialAllocationRequest = createGameBehaviorRequest(
                userId,
                0,
                "INITIAL_ALLOCATION",
                "ALL"
        );
        initialAllocationRequest.setCurrentCash(300_000L);
        initialAllocationRequest.setCurrentStock(200_000L);
        initialAllocationRequest.setCurrentDeposit(500_000L);
        gameActionService.saveGameActionLog(initialAllocationRequest);

        GameBehaviorRequest depositCancelRequest = createGameBehaviorRequest(
                userId,
                1,
                "DEPOSIT_CANCEL",
                "DEPOSIT"
        );
        depositCancelRequest.setCurrentCash(800_000L);
        depositCancelRequest.setCurrentStock(200_000L);
        depositCancelRequest.setCurrentDeposit(0L);
        gameActionService.saveGameActionLog(depositCancelRequest);

        List<ActionLogDto> actionLogs = actionLogMapper.getActionLogsByUserId(userId);

        assertEquals("ACTIVE", actionLogs.get(0).getDepositStatus());
        assertEquals("CANCELLED", actionLogs.get(1).getDepositStatus());
    }

    private ActionLogDto saveDepositCancelAndSecurityBuy(int cancelTick, int buyTick) {
        Long userId = createUser();
        gameActionService.saveGameActionLog(createGameBehaviorRequest(
                userId,
                cancelTick,
                "DEPOSIT_CANCEL",
                "DEPOSIT"
        ));
        gameActionService.saveGameActionLog(createGameBehaviorRequest(
                userId,
                buyTick,
                "BUY",
                "STOCK"
        ));

        List<ActionLogDto> actionLogs = actionLogMapper.getActionLogsByUserId(userId);
        return actionLogs.get(1);
    }

    private GameBehaviorRequest createGameBehaviorRequest(
            Long userId,
            int tick,
            String actionType,
            String assetType) {
        GameBehaviorRequest request = new GameBehaviorRequest();
        request.setUserId(userId);
        request.setScenarioId("SC001");
        request.setTick(tick);
        request.setActionType(actionType);
        request.setAssetType(assetType);
        request.setActionAmount(100_000L);
        request.setCurrentCash(900_000L);
        request.setCurrentStock(100_000L);
        request.setCurrentDeposit(0L);
        return request;
    }

    private Long createUser() {
        String identifier = UUID.randomUUID().toString().substring(0, 8);
        String email = "game-action-" + identifier + "@test.com";

        jdbcTemplate.update(
                "INSERT INTO users "
                        + "(email, password, nickname, birth_date, postal_code, address_line1) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                email,
                "password",
                "game-action-" + identifier,
                "2000-01-01",
                "00000",
                "테스트 주소"
        );

        return jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE email = ?",
                Long.class,
                email
        );
    }
}
