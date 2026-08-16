package org.kkobi.trade.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.trade.dto.HoldingDto;
import org.kkobi.trade.dto.OrderDto;
import org.kkobi.trade.enums.OrderType;
import org.kkobi.trade.exception.TradeErrorCode;
import org.kkobi.trade.exception.TradeException;
import org.kkobi.trade.mapper.HoldingMapper;
import org.kkobi.trade.mapper.OrderMapper;
import org.kkobi.trade.mapper.TradeAccountMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class OrderExecutionService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TradeAccountMapper accountMapper;
    private final HoldingMapper holdingMapper;
    private final OrderMapper orderMapper;
    private final HoldingService holdingService;

    /**
     * 주문을 체결 처리한다. 호출 전 계좌에 이미 FOR UPDATE 락이 걸려 있어야 한다.
     * PENDING이었던 주문은 wasLocked=true로 호출해 잠금을 먼저 해제한다.
     *
     * @param order         체결할 주문 (DB에 이미 insert된 상태)
     * @param executedPrice 체결 단가
     * @param wasLocked     PENDING에서 체결되는 경우 true (잠금 해제 필요)
     */
    public void execute(OrderDto order, long executedPrice, boolean wasLocked) {
        Long accountId = order.getAccountId();
        Long securityId = order.getSecurityId();
        int qty = order.getQuantity();
        long amount = executedPrice * qty;

        if (order.getOrderType() == OrderType.BUY) {
            if (wasLocked) {
                accountMapper.decreaseLocked(accountId, order.getOrderPrice() * qty);
            }
            int updated = accountMapper.decreaseCashBalance(accountId, amount);
            if (updated == 0) {
                throw new TradeException(TradeErrorCode.INSUFFICIENT_CASH);
            }
            holdingService.applyBuy(accountId, securityId, executedPrice, qty);

        } else {
            if (wasLocked) {
                HoldingDto h = holdingMapper.findByAccountAndSecurity(accountId, securityId);
                if (h != null) {
                    holdingMapper.decreaseLocked(h.getHoldingSecurityId(), qty);
                }
            }
            holdingService.applySell(accountId, securityId, qty);
            accountMapper.increaseCashBalance(accountId, amount);
        }

        orderMapper.updateFilled(order.getSecurityOrderId(), executedPrice, LocalDateTime.now(KST));
    }
}
