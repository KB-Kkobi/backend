package org.kkobi.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kkobi.notification.enums.NotificationType;
import org.kkobi.notification.service.NotificationService;
import org.kkobi.trade.dto.TradeAccountDto;
import org.kkobi.trade.dto.TradeStockDto;
import org.kkobi.trade.enums.OrderType;
import org.kkobi.trade.event.TradeOrderFilledEvent;
import org.kkobi.trade.mapper.TradeAccountMapper;
import org.kkobi.trade.mapper.TradeStockMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeNotificationListener {

    private final NotificationService notificationService;
    private final TradeAccountMapper accountMapper;
    private final TradeStockMapper stockMapper;

    // 주문 체결 트랜잭션이 정상 커밋된 후 알림 생성
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void onTraderOrderFilled(TradeOrderFilledEvent event){
        try{
            TradeAccountDto account = accountMapper.findByAccountId(event.accountId());

            if(account == null){
                log.warn(
                        "체결 알림 계좌 조회 실패 accountId={}",
                        event.accountId()
                );
                return;
            }

            TradeStockDto stock = stockMapper.findById(event.securityId());

            if(stock == null){
                log.warn(
                        "체결 알림 종목 조회 실패 securityId={}",
                        event.securityId()
                );
                return;
            }

            NotificationType notificationType;

            String title;

            String message;

            if(event.orderType() == OrderType.BUY){
                notificationType = NotificationType.TRADE_BUY_FILLED;

                title = "매수 체결";

                message = stock.getName()
                        + " "
                        + event.quantity()
                        + "주 매수가 "
                        + String.format(
                        "%,d",
                        event.executedPrice()
                )
                        + "원에 체결됐어요.";

            }else {
                notificationType = NotificationType.TRADE_SELL_FILLED;

                title = "매도 체결";

                message =
                        stock.getName()
                                + " "
                                + event.quantity()
                                + "주 매도가 "
                                + String.format(
                                "%,d",
                                event.executedPrice()
                        )
                                + "원에 체결됐어요.";
            }

            notificationService.createNotification(
                    account.getUserId(),
                    notificationType,
                    title,
                    message,
                    event.securityOrderId()
            );
        } catch (Exception e){
            log.warn(
                    "거래 체결 알림 생성 실패 securityOrderId={} accountId={} error={}",
                    event.securityOrderId(),
                    event.accountId(),
                    e.getMessage()
            );
        }
    }
}
