package org.kkobi.trade.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kkobi.trade.dto.OrderDto;
import org.kkobi.trade.mapper.OrderMapper;
import org.kkobi.trade.mapper.TradeAccountMapper;
import org.kkobi.trade.service.OrderExecutionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 개별 주문 체결을 독립 트랜잭션으로 처리한다.
 * TickMatchingEngine이 직접 @Transactional 메서드를 호출하면 프록시를 우회하므로
 * 별도 스프링 빈으로 분리해 REQUIRES_NEW를 보장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMatchTransactionService {

    private final OrderMapper orderMapper;
    private final TradeAccountMapper accountMapper;
    private final OrderExecutionService orderExecutionService;

    /**
     * 단일 주문 체결을 독립 트랜잭션으로 실행한다.
     * updateFilledConditional이 0을 반환하면 이미 처리된 주문이므로
     * OrderExecutionService.execute 호출 없이 스킵한다.
     *
     * @param order        체결 대상 주문
     * @param currentPrice 현재 체결가
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void matchSingleOrder(OrderDto order, long currentPrice) {
        // FOR UPDATE 락 획득: OrderExecutionService.execute 사전 조건
        accountMapper.findByAccountIdForUpdate(order.getAccountId());

        // PENDING 상태인 경우에만 FILLED로 전환 (중복 체결 방지 가드)
        int affected = orderMapper.updateFilledConditional(
                order.getSecurityOrderId(),
                currentPrice,
                LocalDateTime.now()
        );
        if (affected == 0) {
            log.debug("주문 이미 처리됨, 스킵 securityOrderId={}", order.getSecurityOrderId());
            return;
        }

        // 잠금 해제 + 잔고/보유 처리 (execute 내부의 updateFilled는 이미 FILLED인 동일 행을 재기록하므로 무해)
        orderExecutionService.execute(order, currentPrice, true);
    }
}
