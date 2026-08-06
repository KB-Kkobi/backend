package org.kkobi.external.kis.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// STOMP SUBSCRIBE / UNSUBSCRIBE / DISCONNECT 이벤트를 받아
// StockSubscriptionManager로 라우팅한다.
// destination 형식은 /topic/stocks/{종목코드} 로 고정한다.
@Component
@Log4j2
@RequiredArgsConstructor
public class StompSubscriptionListener {

    private static final Pattern DESTINATION_PATTERN =
            Pattern.compile("^/topic/stocks/(\\d{6})$");

    private final StockSubscriptionManager subscriptionManager;

    // subscriptionId -> stockCode 매핑 (UNSUBSCRIBE 시 destination이 없으므로 필요)
    private final Map<String, String> subscriptionCodeById = new LinkedHashMap<>();
    private final Object lock = new Object();

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        String destination = accessor.getDestination();
        if (sessionId == null || subscriptionId == null || destination == null) {
            return;
        }
        String stockCode = extractStockCode(destination);
        if (stockCode == null) {
            return;
        }
        boolean accepted = subscriptionManager.subscribe(sessionId, stockCode);
        if (accepted) {
            synchronized (lock) {
                subscriptionCodeById.put(subscriptionKey(sessionId, subscriptionId), stockCode);
            }
        }
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        if (sessionId == null || subscriptionId == null) {
            return;
        }
        String stockCode;
        synchronized (lock) {
            stockCode = subscriptionCodeById.remove(subscriptionKey(sessionId, subscriptionId));
        }
        if (stockCode != null) {
            subscriptionManager.unsubscribe(sessionId, stockCode);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId == null) {
            return;
        }
        synchronized (lock) {
            subscriptionCodeById.keySet().removeIf(key -> key.startsWith(sessionId + "|"));
        }
        subscriptionManager.removeSession(sessionId);
    }

    private static String extractStockCode(String destination) {
        Matcher matcher = DESTINATION_PATTERN.matcher(destination);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static String subscriptionKey(String sessionId, String subscriptionId) {
        return sessionId + "|" + subscriptionId;
    }
}
