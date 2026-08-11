package org.kkobi.external.kis.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockSubscriptionManagerTest {

    private RecordingKisClient client;
    private StockSubscriptionManager manager;

    @BeforeEach
    void setUp() {
        client = new RecordingKisClient();
        manager = new StockSubscriptionManager(client);
    }

    @Test
    @DisplayName("첫 구독 시 KIS 업스트림 구독이 한 번 트리거된다.")
    void firstSubscribeTriggersUpstream() {
        boolean accepted = manager.subscribe("s1", "005930");

        assertTrue(accepted);
        assertEquals(List.of("SUB:005930"), client.events);
    }

    @Test
    @DisplayName("같은 종목의 중복 구독은 업스트림을 다시 부르지 않는다.")
    void duplicateSubscribeDoesNotResubscribe() {
        manager.subscribe("s1", "005930");
        manager.subscribe("s2", "005930");

        assertEquals(List.of("SUB:005930"), client.events);
    }

    @Test
    @DisplayName("마지막 구독자가 해제될 때만 KIS 업스트림이 해제된다.")
    void unsubscribeReleasesUpstreamOnlyOnZero() {
        manager.subscribe("s1", "005930");
        manager.subscribe("s2", "005930");

        manager.unsubscribe("s1", "005930");
        assertEquals(List.of("SUB:005930"), client.events);

        manager.unsubscribe("s2", "005930");
        assertEquals(List.of("SUB:005930", "UNSUB:005930"), client.events);
    }

    @Test
    @DisplayName("세션 종료 시 그 세션이 잡고 있던 종목은 refCount가 감소한다.")
    void removeSessionDecrementsHeldCodes() {
        manager.subscribe("s1", "005930");
        manager.subscribe("s1", "000660");
        manager.subscribe("s2", "005930");

        manager.removeSession("s1");

        assertEquals(1, manager.activeCodeCount());
        assertTrue(manager.activeCodes().contains("005930"));
        assertFalse(manager.activeCodes().contains("000660"));
        assertTrue(client.events.contains("UNSUB:000660"));
    }

    @Test
    @DisplayName("서로 다른 종목이 40건을 초과하면 신규 구독은 거절된다.")
    void enforcesFortyLimit() {
        for (int i = 0; i < StockSubscriptionManager.MAX_UNIQUE_CODES; i++) {
            String code = String.format("%06d", i);
            assertTrue(manager.subscribe("s1", code));
        }
        boolean overflow = manager.subscribe("s1", "999999");
        assertFalse(overflow);
        assertEquals(StockSubscriptionManager.MAX_UNIQUE_CODES, manager.activeCodeCount());
    }

    // KisWebSocketClient 대체 구현 — 실제 웹소켓 연결 없이 호출을 기록만 한다.
    static class RecordingKisClient extends KisWebSocketClient {
        final List<String> events = new ArrayList<>();

        RecordingKisClient() {
            super(null, null, null, null, null);
        }

        @Override
        public void subscribeUpstream(String stockCode) {
            events.add("SUB:" + stockCode);
        }

        @Override
        public void unsubscribeUpstream(String stockCode) {
            events.add("UNSUB:" + stockCode);
        }
    }
}
