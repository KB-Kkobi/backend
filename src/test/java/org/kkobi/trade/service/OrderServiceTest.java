package org.kkobi.trade.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.config.RootConfig;
import org.kkobi.external.kis.dto.StockPriceResponse;
import org.kkobi.external.kis.service.StockQuoteService;
import org.kkobi.trade.dto.CancelOrderResult;
import org.kkobi.trade.dto.HoldingDto;
import org.kkobi.trade.dto.PlaceOrderRequest;
import org.kkobi.trade.dto.PlaceOrderResult;
import org.kkobi.trade.enums.OrderMethod;
import org.kkobi.trade.enums.OrderStatus;
import org.kkobi.trade.enums.OrderType;
import org.kkobi.trade.exception.TradeErrorCode;
import org.kkobi.trade.exception.TradeException;
import org.kkobi.trade.mapper.HoldingMapper;
import org.kkobi.trade.mapper.TradeAccountMapper;
import org.kkobi.trade.policy.MarketHoursPolicy;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class, OrderServiceTest.MockConfig.class})
@Transactional
class OrderServiceTest {

    @Configuration
    static class MockConfig {
        @Bean
        @Primary
        public StockQuoteService stockQuoteService() {
            return Mockito.mock(StockQuoteService.class);
        }

        @Bean
        @Primary
        public MarketHoursPolicy marketHoursPolicy() {
            return Mockito.mock(MarketHoursPolicy.class);
        }
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private StockQuoteService stockQuoteService;

    @Autowired
    private MarketHoursPolicy marketHoursPolicy;

    @Autowired
    private TradeAccountMapper tradeAccountMapper;

    @Autowired
    private HoldingMapper holdingMapper;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    void setDataSource(DataSource ds) {
        jdbcTemplate = new JdbcTemplate(ds);
    }

    private static final long CURRENT_PRICE = 70_000L;

    @BeforeEach
    void setUp() {
        Mockito.when(marketHoursPolicy.isMarketOpen()).thenReturn(true);

        StockPriceResponse mockResp = new StockPriceResponse(
                "005930", "삼성전자",
                new BigDecimal(CURRENT_PRICE),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        Mockito.when(stockQuoteService.getCurrentPrice(anyString())).thenReturn(mockResp);
    }

    // =========================================================
    // 테스트 헬퍼
    // =========================================================

    private Long createUserAndAccount(long cashBalance) {
        String identifier = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = "trade-" + identifier + "@test.com";

        jdbcTemplate.update(
                "INSERT INTO users (email, password, nickname, birth_date, postal_code, address_line1) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                email, "password", "trade-" + identifier,
                "2000-01-01", "00000", "테스트 주소"
        );
        Long userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE email = ?", Long.class, email);

        jdbcTemplate.update(
                "INSERT INTO accounts (user_id, seed_money, cash_balance, locked_cash) "
                        + "VALUES (?, ?, ?, ?)",
                userId, cashBalance, cashBalance, 0L
        );
        return userId;
    }

    private Long createSecurity() {
        String ticker = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        String kisCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();

        jdbcTemplate.update(
                "INSERT INTO securities (ticker, name, type, market, kis_code, rt_score, lh_score, rp_score) "
                        + "VALUES (?, ?, 'STOCK', 'KOSPI', ?, 50.00, 50.00, 50.00)",
                ticker, "테스트종목-" + ticker, kisCode
        );
        return jdbcTemplate.queryForObject(
                "SELECT security_id FROM securities WHERE ticker = ?", Long.class, ticker);
    }

    private PlaceOrderRequest buildMarketBuy(Long securityId, int quantity) {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setSecurityId(securityId);
        req.setOrderType(OrderType.BUY);
        req.setOrderMethod(OrderMethod.MARKET);
        req.setQuantity(quantity);
        return req;
    }

    private PlaceOrderRequest buildMarketSell(Long securityId, int quantity) {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setSecurityId(securityId);
        req.setOrderType(OrderType.SELL);
        req.setOrderMethod(OrderMethod.MARKET);
        req.setQuantity(quantity);
        return req;
    }

    private PlaceOrderRequest buildLimitBuy(Long securityId, int quantity, long price) {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setSecurityId(securityId);
        req.setOrderType(OrderType.BUY);
        req.setOrderMethod(OrderMethod.LIMIT);
        req.setQuantity(quantity);
        req.setPrice(price);
        return req;
    }

    private PlaceOrderRequest buildLimitSell(Long securityId, int quantity, long price) {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setSecurityId(securityId);
        req.setOrderType(OrderType.SELL);
        req.setOrderMethod(OrderMethod.LIMIT);
        req.setQuantity(quantity);
        req.setPrice(price);
        return req;
    }

    // =========================================================
    // 시나리오 1: 잔고 부족 매수 거절
    // =========================================================

    @Test
    @DisplayName("잔고가 부족하면 시장가 매수를 거절한다.")
    void placeMarketBuyRejectsWhenInsufficientCash() {
        Long userId = createUserAndAccount(10_000L);  // 잔고 10,000원
        Long securityId = createSecurity();

        PlaceOrderRequest req = buildMarketBuy(securityId, 1);  // 70,000원 필요

        TradeException ex = assertThrows(TradeException.class,
                () -> orderService.placeOrder(userId, req));
        assertEquals(TradeErrorCode.INSUFFICIENT_CASH, ex.getErrorCode());
    }

    // =========================================================
    // 시나리오 2: 보유수량 초과 매도 거절
    // =========================================================

    @Test
    @DisplayName("보유 수량을 초과한 매도 주문을 거절한다.")
    void placeMarketSellRejectsWhenInsufficientQuantity() {
        Long userId = createUserAndAccount(1_000_000L);
        Long securityId = createSecurity();

        // 1주 매수 후 2주 매도 시도
        orderService.placeOrder(userId, buildMarketBuy(securityId, 1));

        TradeException ex = assertThrows(TradeException.class,
                () -> orderService.placeOrder(userId, buildMarketSell(securityId, 2)));
        assertEquals(TradeErrorCode.INSUFFICIENT_QUANTITY, ex.getErrorCode());
    }

    // =========================================================
    // 시나리오 3: 정상 매수 2회 후 평균단가 검증 (내림 처리 경계값)
    // =========================================================

    @Test
    @DisplayName("매수 2회 후 이동평균 단가를 내림으로 올바르게 계산한다.")
    void applyBuyTwiceCalculatesAverageWithFloorDivision() {
        Long userId = createUserAndAccount(1_000_000L);
        Long securityId = createSecurity();

        // 1차 매수: 70,000원 × 1주 → 평균 70,000
        orderService.placeOrder(userId, buildMarketBuy(securityId, 1));

        // 현재가를 71,001원으로 변경 (내림 경계: (70,000 + 71,001) / 2 = 70,500.5 → 70,500)
        Mockito.when(stockQuoteService.getCurrentPrice(anyString()))
                .thenReturn(new StockPriceResponse(
                        "005930", "삼성전자",
                        new BigDecimal("71001"),
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        0L,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
                ));

        // 2차 매수: 71,001원 × 1주
        orderService.placeOrder(userId, buildMarketBuy(securityId, 1));

        Long accountId = tradeAccountMapper.findByUserId(userId).getAccountId();
        HoldingDto holding = holdingMapper.findByAccountAndSecurity(accountId, securityId);

        assertNotNull(holding);
        assertEquals(2, holding.getQuantity());
        // floor((70,000 * 1 + 71,001 * 1) / 2) = floor(70,500.5) = 70,500
        assertEquals(70_500L, holding.getAveragePrice());
    }

    // =========================================================
    // 시나리오 4: 전량 매도 후 quantity=0, 현금 증가 검증
    // =========================================================

    @Test
    @DisplayName("전량 매도 후 보유 수량이 0이 되고 현금이 증가한다.")
    void placeMarketSellAllDecreasesQuantityToZeroAndIncreasesCash() {
        Long userId = createUserAndAccount(1_000_000L);
        Long securityId = createSecurity();

        // 매수: 70,000원 × 2주
        orderService.placeOrder(userId, buildMarketBuy(securityId, 2));

        long cashAfterBuy = tradeAccountMapper.findByUserId(userId).getCashBalance();

        // 전량 매도: 70,000원 × 2주
        orderService.placeOrder(userId, buildMarketSell(securityId, 2));

        Long accountId = tradeAccountMapper.findByUserId(userId).getAccountId();
        HoldingDto holding = holdingMapper.findByAccountAndSecurity(accountId, securityId);
        long cashAfterSell = tradeAccountMapper.findByUserId(userId).getCashBalance();

        assertNotNull(holding);
        assertEquals(0, holding.getQuantity());
        assertEquals(cashAfterBuy + CURRENT_PRICE * 2, cashAfterSell);
    }

    // =========================================================
    // 시나리오 5: 지정가 접수 후 locked_cash / locked_quantity 값 검증
    // =========================================================

    @Test
    @DisplayName("지정가 매수 접수 시 주문 금액만큼 locked_cash가 증가한다.")
    void placeLimitBuyPendingIncreasesLockedCash() {
        Long userId = createUserAndAccount(1_000_000L);
        Long securityId = createSecurity();

        // 지정가 매수: 현재가(70,000)보다 낮은 50,000원에 2주 → PENDING
        PlaceOrderResult result = orderService.placeOrder(userId,
                buildLimitBuy(securityId, 2, 50_000L));

        assertEquals(OrderStatus.PENDING, result.getStatus());

        long lockedCash = tradeAccountMapper.findByUserId(userId).getLockedCash();
        assertEquals(50_000L * 2, lockedCash);
    }

    @Test
    @DisplayName("지정가 매도 접수 시 locked_quantity가 증가한다.")
    void placeLimitSellPendingIncreasesLockedQuantity() {
        Long userId = createUserAndAccount(1_000_000L);
        Long securityId = createSecurity();

        // 먼저 2주 매수
        orderService.placeOrder(userId, buildMarketBuy(securityId, 2));

        // 지정가 매도: 현재가(70,000)보다 높은 90,000원에 2주 → PENDING
        PlaceOrderResult result = orderService.placeOrder(userId,
                buildLimitSell(securityId, 2, 90_000L));

        assertEquals(OrderStatus.PENDING, result.getStatus());

        Long accountId = tradeAccountMapper.findByUserId(userId).getAccountId();
        HoldingDto holding = holdingMapper.findByAccountAndSecurity(accountId, securityId);
        assertNotNull(holding);
        assertEquals(2, holding.getLockedQuantity());
    }

    // =========================================================
    // 시나리오 6: 지정가 취소 후 잠금 완전 해제 검증
    // =========================================================

    @Test
    @DisplayName("지정가 매수 취소 후 locked_cash가 0으로 해제된다.")
    void cancelLimitBuyReleasesLockedCash() {
        Long userId = createUserAndAccount(1_000_000L);
        Long securityId = createSecurity();

        // 지정가 매수 접수 → PENDING
        PlaceOrderResult placed = orderService.placeOrder(userId,
                buildLimitBuy(securityId, 2, 50_000L));
        assertEquals(OrderStatus.PENDING, placed.getStatus());

        // 취소
        CancelOrderResult cancelled = orderService.cancelOrder(userId, placed.getSecurityOrderId());
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());

        long lockedCash = tradeAccountMapper.findByUserId(userId).getLockedCash();
        assertEquals(0L, lockedCash);
    }

    @Test
    @DisplayName("지정가 매도 취소 후 locked_quantity가 0으로 해제된다.")
    void cancelLimitSellReleasesLockedQuantity() {
        Long userId = createUserAndAccount(1_000_000L);
        Long securityId = createSecurity();

        orderService.placeOrder(userId, buildMarketBuy(securityId, 2));

        // 지정가 매도 접수 → PENDING
        PlaceOrderResult placed = orderService.placeOrder(userId,
                buildLimitSell(securityId, 2, 90_000L));
        assertEquals(OrderStatus.PENDING, placed.getStatus());

        // 취소
        CancelOrderResult cancelled = orderService.cancelOrder(userId, placed.getSecurityOrderId());
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());

        Long accountId = tradeAccountMapper.findByUserId(userId).getAccountId();
        HoldingDto holding = holdingMapper.findByAccountAndSecurity(accountId, securityId);
        assertNotNull(holding);
        assertEquals(0, holding.getLockedQuantity());
    }

    // =========================================================
    // 시나리오 7: 지정가 조건 즉시 충족 시 현재가로 체결
    // =========================================================

    @Test
    @DisplayName("지정가 매수 조건이 즉시 충족되면 지정가가 아닌 현재가로 체결된다.")
    void placeLimitBuyImmediatelyFilledAtCurrentPriceNotLimitPrice() {
        Long userId = createUserAndAccount(1_000_000L);
        Long securityId = createSecurity();

        // 현재가 70,000원보다 높은 지정가 80,000원에 매수 → 즉시 체결
        PlaceOrderResult result = orderService.placeOrder(userId,
                buildLimitBuy(securityId, 1, 80_000L));

        assertEquals(OrderStatus.FILLED, result.getStatus());
        // 체결가는 지정가(80,000)가 아닌 현재가(70,000)여야 함
        assertEquals(CURRENT_PRICE, result.getExecutedPrice());
        assertNotNull(result.getExecutedAt());
    }

    @Test
    @DisplayName("지정가 매도 조건이 즉시 충족되면 현재가로 체결된다.")
    void placeLimitSellImmediatelyFilledAtCurrentPrice() {
        Long userId = createUserAndAccount(1_000_000L);
        Long securityId = createSecurity();

        orderService.placeOrder(userId, buildMarketBuy(securityId, 1));

        // 현재가 70,000원보다 낮은 지정가 60,000원에 매도 → 즉시 체결
        PlaceOrderResult result = orderService.placeOrder(userId,
                buildLimitSell(securityId, 1, 60_000L));

        assertEquals(OrderStatus.FILLED, result.getStatus());
        assertEquals(CURRENT_PRICE, result.getExecutedPrice());
    }

    // =========================================================
    // 시나리오 8: 장외 시간 주문 거절
    // =========================================================

    @Test
    @DisplayName("장 외 시간에 주문하면 MARKET_CLOSED 예외가 발생한다.")
    void placeOrderRejectsWhenMarketClosed() {
        Mockito.when(marketHoursPolicy.isMarketOpen()).thenReturn(false);

        Long userId = createUserAndAccount(1_000_000L);
        Long securityId = createSecurity();

        TradeException ex = assertThrows(TradeException.class,
                () -> orderService.placeOrder(userId, buildMarketBuy(securityId, 1)));
        assertEquals(TradeErrorCode.MARKET_CLOSED, ex.getErrorCode());
    }

    // =========================================================
    // 시나리오 9: 동일 계좌 동시 주문 시 잔고 초과 차감 방지
    // =========================================================

    @Test
    @DisplayName("잔고가 한 번의 주문만 가능할 때 두 번째 주문은 거절된다.")
    void placeOrderRejectsSecondOrderWhenBalanceOnlySufficesForOne() {
        // 잔고 정확히 1주 금액(70,000원)
        Long userId = createUserAndAccount(CURRENT_PRICE);
        Long securityId = createSecurity();

        // 첫 번째 주문: 성공
        PlaceOrderResult first = orderService.placeOrder(userId,
                buildMarketBuy(securityId, 1));
        assertEquals(OrderStatus.FILLED, first.getStatus());

        // 두 번째 주문: 잔고 부족으로 실패
        TradeException ex = assertThrows(TradeException.class,
                () -> orderService.placeOrder(userId, buildMarketBuy(securityId, 1)));
        assertEquals(TradeErrorCode.INSUFFICIENT_CASH, ex.getErrorCode());
    }

    // =========================================================
    // 시나리오 10: 존재하지 않는 종목 주문 → SECURITY_NOT_FOUND
    // =========================================================

    @Test
    @DisplayName("존재하지 않는 securityId로 주문 시 SECURITY_NOT_FOUND 예외가 발생한다.")
    void placeOrderRejectsUnknownSecurity() {
        Long userId = createUserAndAccount(1_000_000L);

        PlaceOrderRequest req = buildMarketBuy(999_999L, 1); // 존재하지 않는 ID

        TradeException ex = assertThrows(TradeException.class,
                () -> orderService.placeOrder(userId, req));
        assertEquals(TradeErrorCode.SECURITY_NOT_FOUND, ex.getErrorCode());
    }

    // =========================================================
    // 시나리오 11: 시세 조회 실패 → QUOTE_UNAVAILABLE
    // =========================================================

    @Test
    @DisplayName("시세 조회가 null을 반환하면 QUOTE_UNAVAILABLE 예외가 발생한다.")
    void placeOrderRejectsWhenQuoteUnavailable() {
        Mockito.when(stockQuoteService.getCurrentPrice(anyString())).thenReturn(null);

        Long userId = createUserAndAccount(1_000_000L);
        Long securityId = createSecurity();

        TradeException ex = assertThrows(TradeException.class,
                () -> orderService.placeOrder(userId, buildMarketBuy(securityId, 1)));
        assertEquals(TradeErrorCode.QUOTE_UNAVAILABLE, ex.getErrorCode());
    }

    // =========================================================
    // 시나리오 12: 지정가 매수 — lockedCash 고려한 가용 잔고 부족
    // =========================================================

    @Test
    @DisplayName("가용 잔고(cashBalance - lockedCash)가 부족하면 지정가 매수를 거절한다.")
    void placeLimitBuyRejectsWhenAvailableCashInsufficient() {
        // cashBalance=200,000, lockedCash=150,000 → 가용=50,000
        Long userId = createUserAndAccount(200_000L);
        Long securityId = createSecurity();

        // lockedCash를 150,000으로 설정 — jdbcTemplate으로 accountId 조회해야 MyBatis L1 캐시 오염 방지
        Long accountId = jdbcTemplate.queryForObject(
                "SELECT account_id FROM accounts WHERE user_id = ?", Long.class, userId);
        jdbcTemplate.update("UPDATE accounts SET locked_cash = 150000 WHERE account_id = ?", accountId);

        // 지정가 매수 2주 × 50,000원 = 100,000원 필요 → 가용 50,000 부족
        TradeException ex = assertThrows(TradeException.class,
                () -> orderService.placeOrder(userId, buildLimitBuy(securityId, 2, 50_000L)));
        assertEquals(TradeErrorCode.INSUFFICIENT_CASH, ex.getErrorCode());
    }

    // =========================================================
    // 시나리오 13: 지정가 매도 — lockedQuantity 고려한 매도가능수량 부족
    // =========================================================

    @Test
    @DisplayName("보유 수량 전체가 이미 잠겨 있으면 추가 지정가 매도를 거절한다.")
    void placeLimitSellRejectsWhenAllQuantityLocked() {
        Long userId = createUserAndAccount(1_000_000L);
        Long securityId = createSecurity();

        // 2주 매수 후 지정가 매도 2주 접수 → lockedQuantity=2
        orderService.placeOrder(userId, buildMarketBuy(securityId, 2));
        PlaceOrderResult pending = orderService.placeOrder(userId,
                buildLimitSell(securityId, 2, 90_000L));
        assertEquals(OrderStatus.PENDING, pending.getStatus());

        // 추가 매도 시도: lockedQuantity=2, quantity=2 → sellable=0 → 거절
        TradeException ex = assertThrows(TradeException.class,
                () -> orderService.placeOrder(userId, buildLimitSell(securityId, 1, 90_000L)));
        assertEquals(TradeErrorCode.INSUFFICIENT_QUANTITY, ex.getErrorCode());
    }
}
