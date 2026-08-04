package org.kkobi.game.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.config.RootConfig;
import org.kkobi.game.dto.ActionLogDto;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@Transactional
class ActionLogMapperTest {

    private final ActionLogMapper actionLogMapper;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    ActionLogMapperTest(ActionLogMapper actionLogMapper, DataSource dataSource) {
        this.actionLogMapper = actionLogMapper;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("게임 행동 로그를 저장한다.")
    void saveActionLog() {
        Long userId = createUser();
        ActionLogDto actionLog = createActionLog(userId);

        int savedRowCount = actionLogMapper.saveActionLog(actionLog);

        assertEquals(1, savedRowCount);
        assertNotNull(actionLog.getActionLogId());
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM action_logs WHERE action_log_id = ?",
                        Integer.class,
                        actionLog.getActionLogId()
                )
        );

        List<ActionLogDto> actionLogs = actionLogMapper.getActionLogsByUserId(userId);
        assertEquals(1, actionLogs.size());
        assertEquals(20, actionLogs.get(0).getGameTick());
        assertEquals(100_000L, actionLogs.get(0).getCurrentStock());
    }

    @Test
    @DisplayName("게임 행동 로그를 tick 순서로 조회한다.")
    void getActionLogsByUserIdOrdersByGameTick() {
        Long userId = createUser();
        actionLogMapper.saveActionLog(createActionLog(userId, 20));
        actionLogMapper.saveActionLog(createActionLog(userId, 5));

        List<ActionLogDto> actionLogs = actionLogMapper.getActionLogsByUserId(userId);

        assertEquals(2, actionLogs.size());
        assertEquals(5, actionLogs.get(0).getGameTick());
        assertEquals(20, actionLogs.get(1).getGameTick());
    }

    private Long createUser() {
        String identifier = UUID.randomUUID().toString().substring(0, 8);
        String email = "action-log-" + identifier + "@test.com";

        jdbcTemplate.update(
                "INSERT INTO users "
                        + "(email, password, nickname, birth_date, postal_code, address_line1) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                email,
                "password",
                "action-log-" + identifier,
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

    private ActionLogDto createActionLog(Long userId) {
        return createActionLog(userId, 20);
    }

    private ActionLogDto createActionLog(Long userId, int gameTick) {
        ActionLogDto actionLog = new ActionLogDto();
        actionLog.setUserId(userId);
        actionLog.setGameTick(gameTick);
        actionLog.setActionType("BUY");
        actionLog.setAssetType("STOCK");
        actionLog.setActionAmount(100_000L);
        actionLog.setMarketState("BULL");
        actionLog.setDepositStatus("NONE");
        actionLog.setCurrentCash(900_000L);
        actionLog.setCurrentStock(100_000L);
        actionLog.setCurrentDeposit(0L);
        actionLog.setRtScoreDelta(new BigDecimal("1.25"));
        actionLog.setLhScoreDelta(new BigDecimal("-0.50"));
        actionLog.setRpScoreDelta(new BigDecimal("0.75"));
        return actionLog;
    }
}
