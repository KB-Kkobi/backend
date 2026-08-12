package org.kkobi.trade.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.config.RootConfig;
import org.kkobi.trade.dto.OrderDto;
import org.kkobi.trade.enums.OrderType;
import org.kkobi.trade.mapper.TradeAccountMapper;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class})
@Transactional
class TickMatchingEngineTest {

    @Autowired
    private TickMatchingEngine tickMatchingEngine;

    @Autowired
    private TradeAccountMapper tradeAccountMapper;

    private OrderMatchTransactionService mockTransactionService;
    private JdbcTemplate jdbcTemplate;

    @Autowired
    void setDataSource(DataSource ds) {
        jdbcTemplate = new JdbcTemplate(ds);
    }

    @BeforeEach
    void setUp() {
        mockTransactionService = Mockito.mock(OrderMatchTransactionService.class);
        // private final 필드에 mock 주입 (Spring AOP 프록시 우회 없이 직접 교체)
        ReflectionTestUtils.setField(tickMatchingEngine, "orderMatchTransactionService", mockTransactionService);
    }

    // =========================================================
    // 헬퍼
    // =========================================================

    private Long createUserAndAccount(long cashBalance) {
        String identifier = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = "engine-" + identifier + "@test.com";
        jdbcTemplate.update(
                "INSERT INTO users (email, password, nickname, birth_date, postal_code, address_line1) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                email, "password", "engine-" + identifier, "2000-01-01", "00000", "테스트 주소");
        Long userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE email = ?", Long.class, email);
        jdbcTemplate.update(
                "INSERT INTO accounts (user_id, seed_money, monthly_invest_amount, cash_balance, locked_cash) "
                        + "VALUES (?, ?, ?, ?, ?)",
                userId, cashBalance, 0L, cashBalance, 0L);
        return userId;
    }

    private Long createSecurityWithKisCode(String kisCode) {
        String ticker = "K" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        jdbcTemplate.update(
                "INSERT INTO securities (ticker, name, type, market, kis_code, rt_score, lh_score, rp_score) "
                        + "VALUES (?, ?, 'STOCK', 'KOSPI', ?, 50.00, 50.00, 50.00)",
                ticker, "엔진테스트-" + ticker, kisCode);
        return jdbcTemplate.queryForObject(
                "SELECT security_id FROM securities WHERE ticker = ?", Long.class, ticker);
    }

    private Long insertPendingLimitOrder(Long accountId, Long securityId,
                                         OrderType orderType, long orderPrice, int quantity) {
        jdbcTemplate.update(
                "INSERT INTO security_orders "
                        + "(account_id, security_id, order_type, order_method, order_price, quantity, status, ordered_at) "
                        + "VALUES (?, ?, ?, 'LIMIT', ?, ?, 'PENDING', NOW())",
                accountId, securityId, orderType.name(), orderPrice, quantity);
        return jdbcTemplate.queryForObject(
                "SELECT security_order_id FROM security_orders "
                        + "WHERE account_id = ? AND security_id = ? ORDER BY security_order_id DESC LIMIT 1",
                Long.class, accountId, securityId);
    }

    private Long getAccountIdByUserId(Long userId) {
        return tradeAccountMapper.findByUserId(userId).getAccountId();
    }

    // =========================================================
    // 시나리오 1: BUY LIMIT — 현재가 하락 시 체결 호출
    // =========================================================

    @Test
    @DisplayName("BUY LIMIT orderPrice=10000, 현재가 9800 수신 → matchSingleOrder 호출됨")
    void buyLimitFilledWhenCurrentPriceFallsBelowOrderPrice() {
        String kisCode = "BUY001";
        Long accountId = getAccountIdByUserId(createUserAndAccount(100_000L));
        Long securityId = createSecurityWithKisCode(kisCode);
        insertPendingLimitOrder(accountId, securityId, OrderType.BUY, 10_000L, 1);

        tickMatchingEngine.onTick(kisCode, 9_800L);
        tickMatchingEngine.matchPendingOrders();

        verify(mockTransactionService, times(1)).matchSingleOrder(any(OrderDto.class), anyLong());
    }

    // =========================================================
    // 시나리오 2: BUY LIMIT — 조건 미충족 시세 → 체결 안됨
    // =========================================================

    @Test
    @DisplayName("BUY LIMIT orderPrice=10000, 현재가 10100 수신 → matchSingleOrder 호출 안됨")
    void buyLimitNotFilledWhenCurrentPriceAboveOrderPrice() {
        String kisCode = "BUY002";
        Long accountId = getAccountIdByUserId(createUserAndAccount(100_000L));
        Long securityId = createSecurityWithKisCode(kisCode);
        insertPendingLimitOrder(accountId, securityId, OrderType.BUY, 10_000L, 1);

        tickMatchingEngine.onTick(kisCode, 10_100L);
        tickMatchingEngine.matchPendingOrders();

        verify(mockTransactionService, never()).matchSingleOrder(any(OrderDto.class), anyLong());
    }

    // =========================================================
    // 시나리오 3: SELL LIMIT — 조건 충족 시 체결 호출
    // =========================================================

    @Test
    @DisplayName("SELL LIMIT orderPrice=10000, 현재가 10500 수신 → matchSingleOrder 호출됨")
    void sellLimitFilledWhenCurrentPriceRisesAboveOrderPrice() {
        String kisCode = "SEL001";
        Long accountId = getAccountIdByUserId(createUserAndAccount(100_000L));
        Long securityId = createSecurityWithKisCode(kisCode);
        insertPendingLimitOrder(accountId, securityId, OrderType.SELL, 10_000L, 1);

        tickMatchingEngine.onTick(kisCode, 10_500L);
        tickMatchingEngine.matchPendingOrders();

        verify(mockTransactionService, times(1)).matchSingleOrder(any(OrderDto.class), anyLong());
    }

    // =========================================================
    // 시나리오 4: 틱 미수신 종목은 처리 안됨
    // =========================================================

    @Test
    @DisplayName("틱이 수신되지 않은 종목의 PENDING 주문은 matchSingleOrder 호출 안됨")
    void noTickReceivedThenNoMatchAttempt() {
        String kisCode = "NOTICK";
        Long accountId = getAccountIdByUserId(createUserAndAccount(100_000L));
        Long securityId = createSecurityWithKisCode(kisCode);
        insertPendingLimitOrder(accountId, securityId, OrderType.BUY, 10_000L, 1);

        // onTick 미호출 상태에서 matchPendingOrders만 실행
        tickMatchingEngine.matchPendingOrders();

        verify(mockTransactionService, never()).matchSingleOrder(any(OrderDto.class), anyLong());
    }

    // =========================================================
    // 시나리오 5: CANCELLED 주문은 findPendingLimitBySecurityId 조회 대상 아님 → 체결 시도 안됨
    // =========================================================

    @Test
    @DisplayName("CANCELLED 상태 주문은 틱 수신 후에도 matchSingleOrder 호출 안됨")
    void cancelledOrderIsNotMatchedByEngine() {
        String kisCode = "CAN001";
        Long accountId = getAccountIdByUserId(createUserAndAccount(100_000L));
        Long securityId = createSecurityWithKisCode(kisCode);
        Long orderId = insertPendingLimitOrder(accountId, securityId, OrderType.BUY, 10_000L, 1);

        // 주문을 CANCELLED로 변경 — findPendingLimitBySecurityId 대상에서 제외됨
        jdbcTemplate.update(
                "UPDATE security_orders SET status = 'CANCELLED' WHERE security_order_id = ?", orderId);

        // 체결 조건을 충족하는 틱 수신
        tickMatchingEngine.onTick(kisCode, 9_800L);
        tickMatchingEngine.matchPendingOrders();

        // PENDING이 아니므로 매칭 시도 자체가 없어야 함
        verify(mockTransactionService, never()).matchSingleOrder(any(OrderDto.class), anyLong());
    }

    // =========================================================
    // 시나리오 6: securities 테이블에 없는 kisCode → 조용히 스킵
    // =========================================================

    @Test
    @DisplayName("securities에 등록되지 않은 kisCode 틱 수신 시 matchSingleOrder 호출 안됨")
    void unknownKisCodeIsSkippedGracefully() {
        // securities 미삽입 상태에서 틱 수신
        tickMatchingEngine.onTick("UNKNOWN_CODE_XYZ", 50_000L);
        tickMatchingEngine.matchPendingOrders();

        verify(mockTransactionService, never()).matchSingleOrder(any(OrderDto.class), anyLong());
    }

    // =========================================================
    // 시나리오 7: 동일 종목 주문 2건 — 조건 충족/미충족 혼재
    // =========================================================

    @Test
    @DisplayName("BUY LIMIT 2건 중 조건 충족 1건만 matchSingleOrder 호출됨")
    void onlyFillableOrdersAreMatched() {
        String kisCode = "MIX001";
        Long accountId = getAccountIdByUserId(createUserAndAccount(200_000L));
        Long securityId = createSecurityWithKisCode(kisCode);

        // 충족 주문: orderPrice=10000, 현재가=9500 → 체결
        insertPendingLimitOrder(accountId, securityId, OrderType.BUY, 10_000L, 1);
        // 미충족 주문: orderPrice=9000, 현재가=9500 → 미체결
        insertPendingLimitOrder(accountId, securityId, OrderType.BUY, 9_000L, 1);

        tickMatchingEngine.onTick(kisCode, 9_500L);
        tickMatchingEngine.matchPendingOrders();

        verify(mockTransactionService, times(1)).matchSingleOrder(any(OrderDto.class), anyLong());
    }
}
