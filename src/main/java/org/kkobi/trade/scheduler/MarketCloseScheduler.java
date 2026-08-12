package org.kkobi.trade.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kkobi.trade.dto.HoldingDto;
import org.kkobi.trade.dto.OrderDto;
import org.kkobi.trade.enums.OrderType;
import org.kkobi.trade.mapper.HoldingMapper;
import org.kkobi.trade.mapper.OrderMapper;
import org.kkobi.trade.mapper.TradeAccountMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 장마감(15:30 KST) 시 당일 미체결 PENDING 주문을 EXPIRED로 만료 처리한다.
 * <p>
 * 멱등 보장: updateExpiredBatch의 WHERE status = 'PENDING' 조건으로 인해
 * 이미 EXPIRED 처리된 주문은 재처리되지 않는다.
 * 또한, findTodayPending을 먼저 실행해 PENDING 상태의 주문만 잠금 해제하므로
 * 동일 배치를 두 번 실행해도 두 번째 실행 시 잠금 해제 대상이 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketCloseScheduler {

    private static final String MARKET_CLOSE_CRON = "0 30 15 * * MON-FRI";
    private static final String KST_ZONE = "Asia/Seoul";

    private final OrderMapper orderMapper;
    private final TradeAccountMapper accountMapper;
    private final HoldingMapper holdingMapper;

    /**
     * 매일 평일 15:30 KST에 실행된다.
     * 잠금 해제 → EXPIRED 배치 순서로 처리해 잠금 해제 대상을 명확히 식별한다.
     */
    @Scheduled(cron = MARKET_CLOSE_CRON, zone = KST_ZONE)
    @Transactional
    public void expirePendingOrders() {
        // EXPIRED 처리 전 PENDING 목록 선조회 (잠금 해제 대상 식별)
        List<OrderDto> pendingOrders = orderMapper.findTodayPending();
        if (pendingOrders.isEmpty()) {
            log.info("장마감 배치: 만료 처리할 PENDING 주문 없음");
            return;
        }

        log.info("장마감 배치 시작: PENDING 주문 {}건 만료 처리", pendingOrders.size());

        // 각 주문의 잠금 해제
        for (OrderDto order : pendingOrders) {
            try {
                releaseLock(order);
            } catch (Exception ex) {
                log.warn("잠금 해제 실패 securityOrderId={} error={}",
                        order.getSecurityOrderId(), ex.getMessage());
            }
        }

        // 당일 PENDING 주문 전체 EXPIRED 처리 (WHERE status = 'PENDING'으로 멱등 보장)
        int expiredCount = orderMapper.updateExpiredBatch();
        log.info("장마감 배치 완료: {}건 EXPIRED 처리", expiredCount);
    }

    /**
     * 주문 유형에 따라 잠금을 해제한다.
     * BUY: 계좌의 locked_cash 감소
     * SELL: 보유 종목의 locked_quantity 감소
     */
    private void releaseLock(OrderDto order) {
        if (order.getOrderType() == OrderType.BUY) {
            long lockedAmount = order.getOrderPrice() * order.getQuantity();
            accountMapper.decreaseLocked(order.getAccountId(), lockedAmount);
        } else {
            HoldingDto holding = holdingMapper.findByAccountAndSecurity(
                    order.getAccountId(), order.getSecurityId());
            if (holding != null) {
                holdingMapper.decreaseLocked(holding.getHoldingSecurityId(), order.getQuantity());
            } else {
                log.warn("장마감 잠금 해제: 보유 종목 없음 accountId={} securityId={}",
                        order.getAccountId(), order.getSecurityId());
            }
        }
    }
}
