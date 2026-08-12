package org.kkobi.trade.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kkobi.trade.dto.OrderDto;
import org.kkobi.trade.dto.TradeStockDto;
import org.kkobi.trade.enums.OrderType;
import org.kkobi.trade.mapper.OrderMapper;
import org.kkobi.trade.mapper.TradeStockMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KIS 실시간 틱을 수신해 PENDING 지정가 주문을 체결하는 매칭 엔진.
 * <p>
 * 틱 수신: onTick()으로 최신 가격을 ConcurrentHashMap에 덮어씀.
 * 배치 실행: MATCH_INTERVAL_MS(300ms)마다 맵을 드레인 후 종목별로 PENDING LIMIT 주문을 조회·체결.
 * 각 주문은 독립 트랜잭션(OrderMatchTransactionService.matchSingleOrder)으로 처리하므로
 * 한 주문의 예외가 다른 주문 처리를 막지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TickMatchingEngine {

    private static final long MATCH_INTERVAL_MS = 300L;

    private final TradeStockMapper tradeStockMapper;
    private final OrderMapper orderMapper;
    private final OrderMatchTransactionService orderMatchTransactionService;

    /** stockCode(KIS 종목코드) → 최신 가격 */
    private final ConcurrentHashMap<String, Long> latestPriceMap = new ConcurrentHashMap<>();

    /**
     * KIS 웹소켓 틱 수신 시 호출된다.
     * 동일 종목의 새 틱이 오면 기존 값을 덮어쓴다.
     *
     * @param stockCode KIS 종목코드
     * @param price     체결 단가
     */
    public void onTick(String stockCode, long price) {
        latestPriceMap.put(stockCode, price);
    }

    /**
     * 300ms마다 맵에 쌓인 종목들을 꺼내(드레인) PENDING LIMIT 주문과 매칭한다.
     * AssessmentSchedulingConfig에 @EnableScheduling이 이미 선언되어 있으므로 별도 설정 불필요.
     */
    @Scheduled(fixedDelay = MATCH_INTERVAL_MS)
    public void matchPendingOrders() {
        // 현재 쌓인 종목 스냅샷 드레인 — 처리 중 새로 들어오는 틱은 다음 사이클에 처리
        Map<String, Long> snapshot = drainPriceMap();
        if (snapshot.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Long> entry : snapshot.entrySet()) {
            String stockCode = entry.getKey();
            long currentPrice = entry.getValue();

            try {
                matchOrdersByStockCode(stockCode, currentPrice);
            } catch (Exception ex) {
                log.warn("종목 매칭 중 오류 발생 stockCode={} price={} error={}",
                        stockCode, currentPrice, ex.getMessage());
            }
        }
    }

    private Map<String, Long> drainPriceMap() {
        Map<String, Long> snapshot = new HashMap<>(latestPriceMap);
        latestPriceMap.keySet().removeAll(snapshot.keySet());
        return snapshot;
    }

    private void matchOrdersByStockCode(String stockCode, long currentPrice) {
        TradeStockDto stock = tradeStockMapper.findByKisCode(stockCode);
        if (stock == null) {
            log.debug("securities 테이블에 등록되지 않은 종목코드, 스킵 stockCode={}", stockCode);
            return;
        }

        List<OrderDto> pendingOrders = orderMapper.findPendingLimitBySecurityId(stock.getSecurityId());
        if (pendingOrders.isEmpty()) {
            return;
        }

        for (OrderDto order : pendingOrders) {
            if (!isFillable(order, currentPrice)) {
                continue;
            }

            try {
                orderMatchTransactionService.matchSingleOrder(order, currentPrice);
                // TODO: Path B 성향 채점 연결 미구현
                //       체결 완료 후 SecurityOrderFilledEvent를 발행해야 한다.
                //       KIS 틱에 changeRate/open/high/low가 없으면 별도 REST 조회로 dailyPriceRangeRate를 계산할 것.
                log.info("지정가 주문 체결 securityOrderId={} orderType={} orderPrice={} executedPrice={}",
                        order.getSecurityOrderId(), order.getOrderType(),
                        order.getOrderPrice(), currentPrice);
            } catch (Exception ex) {
                log.warn("주문 체결 실패 securityOrderId={} error={}",
                        order.getSecurityOrderId(), ex.getMessage());
            }
        }
    }

    /**
     * 지정가 체결 가능 여부를 판단한다.
     * BUY: 지정가 >= 현재가 (지정한 가격 이상으로 내려오면 체결)
     * SELL: 지정가 <= 현재가 (지정한 가격 이상으로 올라오면 체결)
     */
    private boolean isFillable(OrderDto order, long currentPrice) {
        if (order.getOrderType() == OrderType.BUY) {
            return order.getOrderPrice() >= currentPrice;
        } else {
            return order.getOrderPrice() <= currentPrice;
        }
    }
}
