package org.kkobi.trade.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.config.RootConfig;
import org.kkobi.trade.mapper.HoldingMapper;
import org.kkobi.trade.mapper.OrderMapper;
import org.kkobi.trade.mapper.TradeAccountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class})
@Transactional
class MarketCloseSchedulerTest {

    @Autowired
    private MarketCloseScheduler marketCloseScheduler;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private TradeAccountMapper tradeAccountMapper;

    @Autowired
    private HoldingMapper holdingMapper;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    void setDataSource(DataSource ds) {
        jdbcTemplate = new JdbcTemplate(ds);
    }

    // =========================================================
    // 테스트 헬퍼
    // =========================================================

    private Long createUserAndAccount(long cashBalance, long lockedCash) {
        String identifier = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = "close-" + identifier + "@test.com";

        jdbcTemplate.update(
                "INSERT INTO users (email, password, nickname, birth_date, postal_code, address_line1) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                email, "password", "close-" + identifier,
                "2000-01-01", "00000", "테스트 주소"
        );
        Long userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE email = ?", Long.class, email);

        jdbcTemplate.update(
                "INSERT INTO accounts (user_id, seed_money, monthly_invest_amount, cash_balance, locked_cash) "
                        + "VALUES (?, ?, ?, ?, ?)",
                userId, cashBalance, 0L, cashBalance, lockedCash
        );
        return userId;
    }

    private Long getAccountIdByUserId(Long userId) {
        return tradeAccountMapper.findByUserId(userId).getAccountId();
    }

    private Long createSecurity() {
        String ticker = "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        String kisCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();

        jdbcTemplate.update(
                "INSERT INTO securities (ticker, name, type, market, kis_code, rt_score, lh_score, rp_score) "
                        + "VALUES (?, ?, 'STOCK', 'KOSPI', ?, 50.00, 50.00, 50.00)",
                ticker, "마감테스트-" + ticker, kisCode
        );
        return jdbcTemplate.queryForObject(
                "SELECT security_id FROM securities WHERE ticker = ?", Long.class, ticker);
    }

    /**
     * 당일 PENDING LIMIT 매수 주문 삽입 (ordered_at = NOW())
     */
    private Long insertTodayPendingLimitBuyOrder(Long accountId, Long securityId,
                                                  long orderPrice, int quantity) {
        jdbcTemplate.update(
                "INSERT INTO security_orders "
                        + "(account_id, security_id, order_type, order_method, order_price, quantity, status, ordered_at) "
                        + "VALUES (?, ?, 'BUY', 'LIMIT', ?, ?, 'PENDING', NOW())",
                accountId, securityId, orderPrice, quantity
        );
        return jdbcTemplate.queryForObject(
                "SELECT security_order_id FROM security_orders "
                        + "WHERE account_id = ? AND security_id = ? ORDER BY security_order_id DESC LIMIT 1",
                Long.class, accountId, securityId);
    }

    private String getOrderStatus(Long securityOrderId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM security_orders WHERE security_order_id = ?",
                String.class, securityOrderId);
    }

    private Long getLockedCash(Long accountId) {
        return jdbcTemplate.queryForObject(
                "SELECT locked_cash FROM accounts WHERE account_id = ?",
                Long.class, accountId);
    }

    // =========================================================
    // 시나리오 1: 장마감 배치 → PENDING → EXPIRED, locked_cash = 0
    // =========================================================

    @Test
    @DisplayName("PENDING BUY LIMIT 주문 1건 — expirePendingOrders 실행 후 EXPIRED, locked_cash=0")
    void expirePendingBuyLimitOrderReleasesLockedCash() {
        long orderPrice = 10_000L;
        int quantity = 2;
        long lockedCash = orderPrice * quantity; // 20,000원 잠금

        Long userId = createUserAndAccount(100_000L, lockedCash);
        Long accountId = getAccountIdByUserId(userId);
        Long securityId = createSecurity();

        Long orderId = insertTodayPendingLimitBuyOrder(accountId, securityId, orderPrice, quantity);

        // 사전 상태 검증
        assertEquals("PENDING", getOrderStatus(orderId));
        assertEquals(lockedCash, getLockedCash(accountId));

        // 장마감 배치 실행
        marketCloseScheduler.expirePendingOrders();

        // 사후 검증
        assertEquals("EXPIRED", getOrderStatus(orderId));
        assertEquals(0L, getLockedCash(accountId));
    }

    // =========================================================
    // 시나리오 2: 배치 2회 실행 멱등성 검증
    // =========================================================

    @Test
    @DisplayName("expirePendingOrders 두 번 실행해도 결과 동일 — 멱등성 보장")
    void expirePendingOrdersIsIdempotent() {
        long orderPrice = 5_000L;
        int quantity = 3;
        long lockedCash = orderPrice * quantity; // 15,000원 잠금

        Long userId = createUserAndAccount(100_000L, lockedCash);
        Long accountId = getAccountIdByUserId(userId);
        Long securityId = createSecurity();

        Long orderId = insertTodayPendingLimitBuyOrder(accountId, securityId, orderPrice, quantity);

        // 1회 실행
        marketCloseScheduler.expirePendingOrders();

        assertEquals("EXPIRED", getOrderStatus(orderId));
        assertEquals(0L, getLockedCash(accountId));

        // 2회 실행: 이미 EXPIRED인 주문은 findTodayPending에서 조회되지 않으므로
        // locked_cash가 음수가 되거나 다른 부작용 없음
        marketCloseScheduler.expirePendingOrders();

        assertEquals("EXPIRED", getOrderStatus(orderId));
        assertEquals(0L, getLockedCash(accountId));
    }

    // =========================================================
    // 시나리오 3: PENDING 주문 없을 때 배치 실행 — 정상 종료
    // =========================================================

    @Test
    @DisplayName("PENDING 주문 없을 때 expirePendingOrders 실행해도 예외 없이 종료됨")
    void expirePendingOrdersWithNoPendingOrdersCompletesNormally() {
        // PENDING 주문 미삽입 상태에서 실행
        // 예외 없이 정상 종료되는지만 검증
        marketCloseScheduler.expirePendingOrders();
    }

    // =========================================================
    // 시나리오 4-1: PENDING SELL LIMIT 주문 만료 → locked_quantity 해제
    // =========================================================

    @Test
    @DisplayName("PENDING SELL LIMIT 주문 만료 시 holding_securities.locked_quantity가 0이 된다.")
    void expirePendingSellLimitReleasesLockedQuantity() {
        Long userId = createUserAndAccount(500_000L, 0L);
        Long accountId = getAccountIdByUserId(userId);
        Long securityId = createSecurity();

        // 보유 종목 삽입 (2주 보유, 2주 잠금)
        jdbcTemplate.update(
                "INSERT INTO holding_securities (account_id, security_id, quantity, average_price, locked_quantity) "
                        + "VALUES (?, ?, ?, ?, ?)",
                accountId, securityId, 2, 50_000L, 2L);
        Long holdingId = jdbcTemplate.queryForObject(
                "SELECT holding_security_id FROM holding_securities WHERE account_id = ? AND security_id = ?",
                Long.class, accountId, securityId);

        // 당일 PENDING SELL LIMIT 주문 삽입
        jdbcTemplate.update(
                "INSERT INTO security_orders "
                        + "(account_id, security_id, order_type, order_method, order_price, quantity, status, ordered_at) "
                        + "VALUES (?, ?, 'SELL', 'LIMIT', ?, ?, 'PENDING', NOW())",
                accountId, securityId, 60_000L, 2);
        Long orderId = jdbcTemplate.queryForObject(
                "SELECT security_order_id FROM security_orders "
                        + "WHERE account_id = ? AND security_id = ? ORDER BY security_order_id DESC LIMIT 1",
                Long.class, accountId, securityId);

        // 장마감 배치 실행
        marketCloseScheduler.expirePendingOrders();

        // 검증: 주문 EXPIRED, locked_quantity=0
        assertEquals("EXPIRED", getOrderStatus(orderId));
        Long lockedQty = jdbcTemplate.queryForObject(
                "SELECT locked_quantity FROM holding_securities WHERE holding_security_id = ?",
                Long.class, holdingId);
        assertEquals(0L, lockedQty);
    }

    // =========================================================
    // 시나리오 4-2: PENDING 2건 중 1건만 당일 → 당일 것만 EXPIRED
    // =========================================================

    @Test
    @DisplayName("전일 PENDING 주문은 장마감 배치 대상이 아님 — 당일 주문만 EXPIRED")
    void expirePendingOrdersOnlyAffectsTodayOrders() {
        Long userId = createUserAndAccount(200_000L, 10_000L);
        Long accountId = getAccountIdByUserId(userId);
        Long securityId = createSecurity();

        // 당일 PENDING 주문
        Long todayOrderId = insertTodayPendingLimitBuyOrder(accountId, securityId, 5_000L, 2);

        // 전일 PENDING 주문 (ordered_at = 어제)
        jdbcTemplate.update(
                "INSERT INTO security_orders "
                        + "(account_id, security_id, order_type, order_method, order_price, quantity, status, ordered_at) "
                        + "VALUES (?, ?, 'BUY', 'LIMIT', ?, ?, 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY))",
                accountId, securityId, 5_000L, 2
        );
        Long yesterdayOrderId = jdbcTemplate.queryForObject(
                "SELECT security_order_id FROM security_orders "
                        + "WHERE account_id = ? AND security_id = ? ORDER BY security_order_id DESC LIMIT 1",
                Long.class, accountId, securityId);

        marketCloseScheduler.expirePendingOrders();

        assertEquals("EXPIRED", getOrderStatus(todayOrderId));
        assertEquals("PENDING", getOrderStatus(yesterdayOrderId));
    }
}
