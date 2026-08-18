package org.kkobi.assessment.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.assessment.config.AssessmentDatabaseTestConfig;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorDto;
import org.kkobi.assessment.enums.AssessmentPeriodType;
import org.kkobi.assessment.mapper.VirtualInvestmentBehaviorMapper;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AssessmentDatabaseTestConfig.class)
class VirtualInvestmentPeriodAssessmentServiceIntegrationTest {

    private static final LocalDate ASSESSMENT_DATE = LocalDate.of(2026, 8, 12);

    @Autowired
    private VirtualInvestmentPeriodAssessmentService assessmentService;

    @Autowired
    private AssessmentSettlementService assessmentSettlementService;

    @Autowired
    private VirtualInvestmentBehaviorMapper behaviorMapper;

    private JdbcTemplate jdbcTemplate;
    private Long userId;
    private Long accountId;
    private Long securityId;

    @Autowired
    void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void setUp() {
        String identifier = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        createUserAndAccount(identifier);
        createSecurity(identifier);
        createNormalPartialSellHistory();
        createCashRetentionSnapshots();
    }

    @AfterEach
    void cleanUp() {
        if (accountId != null) {
            jdbcTemplate.update(
                    "DELETE FROM assessment_settlements WHERE account_id = ?",
                    accountId
            );
        }
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM results WHERE user_id = ?", userId);
        }
        if (accountId != null) {
            jdbcTemplate.update(
                    "DELETE FROM account_daily_snapshots WHERE account_id = ?",
                    accountId
            );
            jdbcTemplate.update("DELETE FROM security_orders WHERE account_id = ?", accountId);
            jdbcTemplate.update("DELETE FROM accounts WHERE account_id = ?", accountId);
        }
        if (securityId != null) {
            jdbcTemplate.update(
                    "DELETE FROM security_daily_prices WHERE security_id = ?",
                    securityId
            );
            jdbcTemplate.update("DELETE FROM securities WHERE security_id = ?", securityId);
        }
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", userId);
        }
    }

    @Test
    @DisplayName("실제 행동 이력을 조회해 EMA 결과와 기존 일일 정산을 저장하고 재실행을 차단한다.")
    void calculateFollowUpAssessmentAndPreventDuplicateResult() {
        List<VirtualInvestmentBehaviorDto> behaviors = behaviorMapper
                .getPreviousVirtualInvestmentBehaviors(
                        accountId,
                        Timestamp.valueOf(ASSESSMENT_DATE.plusDays(1).atStartOfDay())
                );

        assertEquals(2, behaviors.size());
        assertEquals("SELL", behaviors.get(0).getActionType());
        assertEquals(new BigDecimal("10100.00"), behaviors.get(0).getCurrentClosePrice());
        assertEquals(new BigDecimal("10000.00"), behaviors.get(0).getPreviousClosePrice());

        int firstAssessmentCount = assessmentService.calculateDailyAssessments(ASSESSMENT_DATE);
        int secondAssessmentCount = assessmentService.calculateDailyAssessments(ASSESSMENT_DATE);

        assertEquals(1, firstAssessmentCount);
        assertEquals(0, secondAssessmentCount);
        assertEquals(1L, countResults());
        assertLatestScore("50.00", "51.67", "50.00");

        String settlementType = jdbcTemplate.queryForObject(
                "SELECT assessment_period_type FROM assessment_settlements "
                        + "WHERE account_id = ? AND period_date = ?",
                String.class,
                accountId,
                ASSESSMENT_DATE
        );
        String settlementStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM assessment_settlements "
                        + "WHERE account_id = ? AND period_date = ?",
                String.class,
                accountId,
                ASSESSMENT_DATE
        );

        assertEquals(AssessmentPeriodType.DAILY_ASSESSMENT_BATCH.name(), settlementType);
        assertEquals("COMPLETED", settlementStatus);
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_settlements WHERE account_id = ?",
                Long.class,
                accountId
        ));
    }

    @Test
    @DisplayName("정산 완료가 실패하면 EMA 결과와 일일 정산 레코드를 함께 롤백한다.")
    void rollbackResultAndSettlementWhenCompletionFails() throws Exception {
        VirtualInvestmentPeriodAssessmentService target =
                AopTestUtils.getUltimateTargetObject(assessmentService);
        AssessmentSettlementService settlementSpy = spy(assessmentSettlementService);
        ReflectionTestUtils.setField(target, "assessmentSettlementService", settlementSpy);
        doThrow(new IllegalStateException("정산 완료 실패"))
                .when(settlementSpy)
                .completeAssessment(
                        accountId,
                        AssessmentPeriodType.DAILY_ASSESSMENT_BATCH,
                        ASSESSMENT_DATE
                );

        try {
            assertEquals(true, AopUtils.isAopProxy(assessmentService));
            assertThrows(
                    IllegalStateException.class,
                    () -> assessmentService.calculateDailyAssessments(ASSESSMENT_DATE)
            );
        } finally {
            ReflectionTestUtils.setField(
                    target,
                    "assessmentSettlementService",
                    assessmentSettlementService
            );
        }

        assertEquals(0L, countResults());
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_settlements WHERE account_id = ?",
                Long.class,
                accountId
        ));
    }

    private void createUserAndAccount(String identifier) {
        String email = "assessment-period-" + identifier + "@test.com";
        jdbcTemplate.update(
                "INSERT INTO users "
                        + "(email, password, nickname, birth_date, postal_code, address_line1) "
                        + "VALUES (?, 'password', ?, '2000-01-01', '00000', '테스트 주소')",
                email,
                "period-" + identifier
        );
        userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE email = ?",
                Long.class,
                email
        );
        jdbcTemplate.update(
                "INSERT INTO accounts (user_id, seed_money, cash_balance, locked_cash) "
                        + "VALUES (?, 1000000, 300000, 0)",
                userId
        );
        accountId = jdbcTemplate.queryForObject(
                "SELECT account_id FROM accounts WHERE user_id = ?",
                Long.class,
                userId
        );
    }

    private void createSecurity(String identifier) {
        String ticker = "AP" + identifier.substring(0, 6).toUpperCase();
        jdbcTemplate.update(
                "INSERT INTO securities "
                        + "(ticker, kis_code, name, type, market, rt_score, lh_score, rp_score) "
                        + "VALUES (?, ?, ?, 'STOCK', 'KOSPI', 50.00, 50.00, 50.00)",
                ticker,
                ticker,
                "정산테스트-" + ticker
        );
        securityId = jdbcTemplate.queryForObject(
                "SELECT security_id FROM securities WHERE ticker = ?",
                Long.class,
                ticker
        );
    }

    private void createNormalPartialSellHistory() {
        insertFilledOrder("BUY", 100, 10_000L, "2026-08-01 10:00:00");
        insertFilledOrder("SELL", 30, 10_100L, "2026-08-10 10:00:00");

        insertDailyPrice("2026-08-09", 10_000L, 10_000L, 10_100L, 9_900L);
        insertDailyPrice("2026-08-10", 10_000L, 10_100L, 10_200L, 9_900L);
    }

    private void insertFilledOrder(
            String orderType,
            int quantity,
            long executedPrice,
            String executedAt) {
        jdbcTemplate.update(
                "INSERT INTO security_orders "
                        + "(account_id, security_id, order_type, order_method, executed_price, "
                        + "quantity, status, ordered_at, executed_at) "
                        + "VALUES (?, ?, ?, 'MARKET', ?, ?, 'FILLED', ?, ?)",
                accountId,
                securityId,
                orderType,
                executedPrice,
                quantity,
                executedAt,
                executedAt
        );
    }

    private void insertDailyPrice(
            String tradeDate,
            long openPrice,
            long closePrice,
            long highPrice,
            long lowPrice) {
        jdbcTemplate.update(
                "INSERT INTO security_daily_prices "
                        + "(security_id, trade_date, open_price, close_price, high_price, low_price, volume) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 1000)",
                securityId,
                tradeDate,
                openPrice,
                closePrice,
                highPrice,
                lowPrice
        );
    }

    private void createCashRetentionSnapshots() {
        insertSnapshot("2026-08-11");
        insertSnapshot("2026-08-12");
    }

    private void insertSnapshot(String snapshotDate) {
        jdbcTemplate.update(
                "INSERT INTO account_daily_snapshots "
                        + "(account_id, snapshot_date, current_cash, current_stock_principal, "
                        + "current_deposit, total_invested_principal, cash_ratio) "
                        + "VALUES (?, ?, 300000, 600000, 100000, 1000000, 30.00)",
                accountId,
                snapshotDate
        );
    }

    private long countResults() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM results WHERE user_id = ?",
                Long.class,
                userId
        );
    }

    private void assertLatestScore(
            String expectedRt,
            String expectedLh,
            String expectedRp) {
        BigDecimal rtScore = jdbcTemplate.queryForObject(
                "SELECT rt_score FROM results WHERE user_id = ? ORDER BY result_id DESC LIMIT 1",
                BigDecimal.class,
                userId
        );
        BigDecimal lhScore = jdbcTemplate.queryForObject(
                "SELECT lh_score FROM results WHERE user_id = ? ORDER BY result_id DESC LIMIT 1",
                BigDecimal.class,
                userId
        );
        BigDecimal rpScore = jdbcTemplate.queryForObject(
                "SELECT rp_score FROM results WHERE user_id = ? ORDER BY result_id DESC LIMIT 1",
                BigDecimal.class,
                userId
        );

        assertNotNull(rtScore);
        assertEquals(0, new BigDecimal(expectedRt).compareTo(rtScore));
        assertEquals(0, new BigDecimal(expectedLh).compareTo(lhScore));
        assertEquals(0, new BigDecimal(expectedRp).compareTo(rpScore));
    }
}
