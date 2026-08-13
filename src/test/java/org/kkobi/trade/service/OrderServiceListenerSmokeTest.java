package org.kkobi.trade.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.assessment.service.SecurityOrderAssessmentListener;
import org.kkobi.assessment.service.VirtualInvestmentAssessmentService;
import org.kkobi.config.RootConfig;
import org.kkobi.external.kis.dto.StockPriceResponse;
import org.kkobi.external.kis.service.StockQuoteService;
import org.kkobi.trade.dto.PlaceOrderRequest;
import org.kkobi.trade.enums.OrderMethod;
import org.kkobi.trade.enums.OrderType;
import org.kkobi.trade.policy.MarketHoursPolicy;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * placeOrder() → AFTER_COMMIT 리스너 발화 스모크 테스트.
 *
 * @Transactional 없이 실행해 placeOrder() 내부 TX가 실제로 커밋되도록 한다.
 * assessmentService 를 spy로 감싸서 리스너 발화와 예외 발생 여부를 직접 검증한다.
 *
 * 픽스처 값 근거:
 *   dailyPriceRangeRate = (high - low) / open × 100
 *                       = (10_200 - 9_800) / 10_100 × 100 ≈ 3.96%
 *   → 5% 미만이므로 MarketState.VOLATILE 로 분류되지 않는다.
 *   currentPriceChangeRate = -6.0 ≤ -5 → MarketState.CRASH → CRASH_BUY 규칙 발화.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class})
class OrderServiceListenerSmokeTest {

    @Autowired private OrderService orderService;
    @Autowired private StockQuoteService originalQuoteService;
    @Autowired private MarketHoursPolicy originalMarketHoursPolicy;
    @Autowired private SecurityOrderAssessmentListener listener;
    @Autowired private VirtualInvestmentAssessmentService assessmentService;

    private JdbcTemplate jdbc;

    @Autowired
    void setDataSource(DataSource ds) { this.jdbc = new JdbcTemplate(ds); }

    private Long userId;
    private Long accountId;
    private Long securityId;
    private VirtualInvestmentAssessmentService originalAssessmentService;
    private VirtualInvestmentAssessmentService spyService;
    private AtomicReference<Throwable> capturedAssessmentEx;

    private static final long INITIAL_CASH = 1_000_000L;
    private static final long PRICE        = 10_000L;
    private static final int  QTY          = 1;

    @BeforeEach
    void setUp() {
        String uid     = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email   = "smoke-" + uid + "@test.com";
        String ticker  = "SK" + uid.substring(0, 4).toUpperCase();
        String kisCode = "SM" + uid.substring(0, 6).toUpperCase();

        jdbc.update(
                "INSERT INTO users (email, password, nickname, birth_date, postal_code, address_line1) " +
                "VALUES (?, 'pw', ?, '2000-01-01', '00000', '주소')",
                email, "닉-" + uid);
        userId = jdbc.queryForObject(
                "SELECT user_id FROM users WHERE email = ?", Long.class, email);

        jdbc.update(
                "INSERT INTO accounts (user_id, seed_money, cash_balance, locked_cash) " +
                        "VALUES (?, ?, ?, 0)",
                userId, INITIAL_CASH, INITIAL_CASH);
        accountId = jdbc.queryForObject(
                "SELECT account_id FROM accounts WHERE user_id = ?", Long.class, userId);

        jdbc.update(
                "INSERT INTO securities (ticker, name, type, market, kis_code, rt_score, lh_score, rp_score) " +
                "VALUES (?, ?, 'STOCK', 'KOSPI', ?, 50.00, 50.00, 50.00)",
                ticker, "스모크-" + ticker, kisCode);
        securityId = jdbc.queryForObject(
                "SELECT security_id FROM securities WHERE ticker = ?", Long.class, ticker);

        StockPriceResponse fakePrice = new StockPriceResponse(
                kisCode, "스모크",
                BigDecimal.valueOf(PRICE),
                BigDecimal.valueOf(-600),
                BigDecimal.valueOf(-6.0),   // changeRate = -6% → CRASH_RATE(-5) 충족
                100L,
                BigDecimal.valueOf(10_100), // open
                BigDecimal.valueOf(10_200), // high — range ≈ 3.96%, VOLATILE(5%) 미해당
                BigDecimal.valueOf(9_800),  // low
                BigDecimal.valueOf(10_600)
        );
        StockQuoteService mockQuote = Mockito.mock(StockQuoteService.class);
        Mockito.when(mockQuote.getCurrentPrice(kisCode)).thenReturn(fakePrice);
        ReflectionTestUtils.setField(orderService, "stockQuoteService", mockQuote);

        MarketHoursPolicy mockPolicy = Mockito.mock(MarketHoursPolicy.class);
        Mockito.when(mockPolicy.isMarketOpen()).thenReturn(true);
        ReflectionTestUtils.setField(orderService, "marketHoursPolicy", mockPolicy);

        // assessmentService spy: 실제 타깃을 꺼내 spy로 감싸고 리스너에 주입.
        // doAnswer 로 내부 예외를 캡처해 검증 0에서 실패로 전환한다.
        VirtualInvestmentAssessmentService realTarget =
                AopTestUtils.getUltimateTargetObject(assessmentService);
        originalAssessmentService = realTarget;
        spyService = Mockito.spy(realTarget);
        capturedAssessmentEx = new AtomicReference<>();
        Mockito.doAnswer(inv -> {
            try {
                return inv.callRealMethod();
            } catch (Throwable t) {
                capturedAssessmentEx.set(t);
                throw t;
            }
        }).when(spyService).updateVirtualInvestmentAssessment(any());
        ReflectionTestUtils.setField(listener, "assessmentService", spyService);
    }

    @AfterEach
    void cleanUp() {
        if (userId    != null) jdbc.update("DELETE FROM results WHERE user_id = ?", userId);
        if (accountId != null) {
            jdbc.update("DELETE FROM holding_securities WHERE account_id = ?", accountId);
            jdbc.update("DELETE FROM security_orders WHERE account_id = ?", accountId);
            jdbc.update("DELETE FROM accounts WHERE account_id = ?", accountId);
        }
        if (userId    != null) jdbc.update("DELETE FROM users WHERE user_id = ?", userId);
        if (securityId != null) jdbc.update("DELETE FROM securities WHERE security_id = ?", securityId);

        if (originalAssessmentService != null) {
            ReflectionTestUtils.setField(listener, "assessmentService", originalAssessmentService);
        }
        ReflectionTestUtils.setField(orderService, "stockQuoteService", originalQuoteService);
        ReflectionTestUtils.setField(orderService, "marketHoursPolicy", originalMarketHoursPolicy);
    }

    @Test
    @DisplayName("placeOrder() 체결 후 AFTER_COMMIT 리스너가 발화해 CRASH_BUY 규칙이 적용되고 results 행이 1건 저장된다")
    void placeMarketBuyOrder_afterCommitListenerFires_crashBuyRuleApplied_resultSaved() {
        assertNotNull(listener, "SecurityOrderAssessmentListener 가 Spring 빈으로 등록됐어야 한다");

        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setSecurityId(securityId);
        req.setOrderType(OrderType.BUY);
        req.setOrderMethod(OrderMethod.MARKET);
        req.setQuantity(QTY);

        orderService.placeOrder(userId, req);

        // 진단: 주문 체결 확인
        Long filledCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM security_orders WHERE account_id = ? AND status = 'FILLED'",
                Long.class, accountId);
        assertEquals(1L, filledCount, "[진단] 주문이 FILLED 됐어야 한다");

        // 검증 0: 리스너 내부 예외 없음 — 예외 발생 시 이 줄에서 실패
        assertNull(capturedAssessmentEx.get(),
                "assessmentService 내부에서 예외 발생: " + capturedAssessmentEx.get());

        // 검증 1: 리스너 발화 → assessmentService.updateVirtualInvestmentAssessment() 호출
        verify(spyService, atLeastOnce()).updateVirtualInvestmentAssessment(any());

        // 검증 2: 체결 후 잔액 = INITIAL - PRICE * QTY
        // AFTER_COMMIT 보장: 리스너 실행 시 TX가 이미 커밋됐으므로 DB 잔액 = 체결 후 값
        long cashAfter = jdbc.queryForObject(
                "SELECT cash_balance FROM accounts WHERE account_id = ?", Long.class, accountId);
        assertEquals(INITIAL_CASH - PRICE * QTY, cashAfter,
                "BUY 체결 후 잔액이 차감됐어야 한다 (리스너가 이 체결 후 값을 읽었음)");

        // 검증 3: CRASH_BUY 규칙 발화 → results 행 1건 저장
        Long resultCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM results WHERE user_id = ?", Long.class, userId);
        assertEquals(1L, resultCount,
                "CRASH_BUY 규칙이 발화했으므로 results 행이 1건 저장됐어야 한다");
    }
}
