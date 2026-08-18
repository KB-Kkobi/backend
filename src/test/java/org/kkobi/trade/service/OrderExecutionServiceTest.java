package org.kkobi.trade.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.trade.dto.OrderDto;
import org.kkobi.trade.enums.OrderType;
import org.kkobi.trade.event.TradeOrderFilledEvent;
import org.kkobi.trade.mapper.HoldingMapper;
import org.kkobi.trade.mapper.OrderMapper;
import org.kkobi.trade.mapper.TradeAccountMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExecutionServiceTest {

    @Mock
    private TradeAccountMapper accountMapper;

    @Mock
    private HoldingMapper holdingMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private HoldingService holdingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OrderExecutionService orderExecutionService;


    @BeforeEach
    void setUp() {

        orderExecutionService =
                new OrderExecutionService(
                        accountMapper,
                        holdingMapper,
                        orderMapper,
                        holdingService,
                        eventPublisher
                );
    }


    @Test
    @DisplayName("매수 체결 완료 후 체결 이벤트를 발행한다")
    void executeBuy_publishesTradeOrderFilledEvent() {

        OrderDto order = new OrderDto();

        order.setSecurityOrderId(100L);
        order.setAccountId(3L);
        order.setSecurityId(1L);
        order.setOrderType(OrderType.BUY);
        order.setQuantity(2);
        order.setOrderPrice(70000L);


        when(
                accountMapper.decreaseCashBalance(
                        3L,
                        144000L
                )
        ).thenReturn(1);


        orderExecutionService.execute(
                order,
                72000L,
                false
        );


        // 실제 매수 처리 확인
        verify(accountMapper)
                .decreaseCashBalance(
                        3L,
                        144000L
                );

        verify(holdingService)
                .applyBuy(
                        3L,
                        1L,
                        72000L,
                        2
                );

        verify(orderMapper)
                .updateFilled(
                        eq(100L),
                        eq(72000L),
                        any()
                );


        // 체결 이벤트 확인
        ArgumentCaptor<Object> eventCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(eventPublisher)
                .publishEvent(
                        eventCaptor.capture()
                );

        Object publishedEvent =
                eventCaptor.getValue();

        assertInstanceOf(
                TradeOrderFilledEvent.class,
                publishedEvent
        );

        TradeOrderFilledEvent event =
                (TradeOrderFilledEvent) publishedEvent;

        assertEquals(
                3L,
                event.accountId()
        );

        assertEquals(
                100L,
                event.securityOrderId()
        );

        assertEquals(
                1L,
                event.securityId()
        );

        assertEquals(
                OrderType.BUY,
                event.orderType()
        );

        assertEquals(
                2,
                event.quantity()
        );

        assertEquals(
                72000L,
                event.executedPrice()
        );
    }


    @Test
    @DisplayName("매도 체결 완료 후 체결 이벤트를 발행한다")
    void executeSell_publishesTradeOrderFilledEvent() {

        OrderDto order = new OrderDto();

        order.setSecurityOrderId(101L);
        order.setAccountId(3L);
        order.setSecurityId(1L);
        order.setOrderType(OrderType.SELL);
        order.setQuantity(1);
        order.setOrderPrice(73000L);


        orderExecutionService.execute(
                order,
                73500L,
                false
        );


        // 실제 매도 처리 확인
        verify(holdingService)
                .applySell(
                        3L,
                        1L,
                        1
                );

        verify(accountMapper)
                .increaseCashBalance(
                        3L,
                        73500L
                );

        verify(orderMapper)
                .updateFilled(
                        eq(101L),
                        eq(73500L),
                        any()
                );


        // 체결 이벤트 확인
        ArgumentCaptor<Object> eventCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(eventPublisher)
                .publishEvent(
                        eventCaptor.capture()
                );

        TradeOrderFilledEvent event =
                assertInstanceOf(
                        TradeOrderFilledEvent.class,
                        eventCaptor.getValue()
                );

        assertEquals(
                3L,
                event.accountId()
        );

        assertEquals(
                101L,
                event.securityOrderId()
        );

        assertEquals(
                OrderType.SELL,
                event.orderType()
        );

        assertEquals(
                1,
                event.quantity()
        );

        assertEquals(
                73500L,
                event.executedPrice()
        );
    }


    @Test
    @DisplayName("매수 체결 실패 시 체결 이벤트를 발행하지 않는다")
    void executeBuy_doesNotPublishEvent_whenExecutionFails() {

        OrderDto order = new OrderDto();

        order.setSecurityOrderId(102L);
        order.setAccountId(3L);
        order.setSecurityId(1L);
        order.setOrderType(OrderType.BUY);
        order.setQuantity(1);
        order.setOrderPrice(72000L);


        when(
                accountMapper.decreaseCashBalance(
                        3L,
                        72000L
                )
        ).thenReturn(0);


        assertThrows(
                RuntimeException.class,
                () -> orderExecutionService.execute(
                        order,
                        72000L,
                        false
                )
        );


        // 체결 실패했으므로 이벤트 발행 X
        verify(
                eventPublisher,
                never()
        ).publishEvent(
                any()
        );

        verify(
                orderMapper,
                never()
        ).updateFilled(
                anyLong(),
                anyLong(),
                any()
        );
    }
}