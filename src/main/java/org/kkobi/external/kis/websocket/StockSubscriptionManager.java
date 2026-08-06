package org.kkobi.external.kis.websocket;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// STOMP 세션별로 구독 중인 종목을 추적하고,
// 종목별 총 구독자 수(refCount)에 따라 KIS 업스트림 구독을 켜고 끈다.
// KIS 세션당 등록 가능한 종목은 최대 40건이므로 초과 요청은 무시하고 경고를 남긴다.
@Component
@Log4j2
public class StockSubscriptionManager {

    static final int MAX_UNIQUE_CODES = 40;

    private final KisWebSocketClient kisWebSocketClient;
    private final Map<String, Integer> refCountByCode = new LinkedHashMap<>();
    private final Map<String, Set<String>> codesBySession = new LinkedHashMap<>();
    private final Object lock = new Object();

    @Autowired
    public StockSubscriptionManager(@Lazy KisWebSocketClient kisWebSocketClient) {
        this.kisWebSocketClient = kisWebSocketClient;
    }

    public boolean subscribe(String sessionId, String stockCode) {
        synchronized (lock) {
            int currentRef = refCountByCode.getOrDefault(stockCode, 0);
            boolean firstTime = currentRef == 0;

            if (firstTime && refCountByCode.size() >= MAX_UNIQUE_CODES) {
                log.warn("KIS 실시간 구독 한도(40) 초과 - 요청 거절 code={} session={}",
                        stockCode, sessionId);
                return false;
            }

            refCountByCode.put(stockCode, currentRef + 1);
            codesBySession.computeIfAbsent(sessionId, k -> new HashSet<>()).add(stockCode);

            if (firstTime) {
                kisWebSocketClient.subscribeUpstream(stockCode);
                log.debug("KIS 업스트림 구독 요청 code={}", stockCode);
            }
            return true;
        }
    }

    public void unsubscribe(String sessionId, String stockCode) {
        synchronized (lock) {
            Set<String> codes = codesBySession.get(sessionId);
            if (codes == null || !codes.remove(stockCode)) {
                return;
            }
            if (codes.isEmpty()) {
                codesBySession.remove(sessionId);
            }
            decrementRefCount(stockCode);
        }
    }

    public void removeSession(String sessionId) {
        synchronized (lock) {
            Set<String> codes = codesBySession.remove(sessionId);
            if (codes == null) {
                return;
            }
            for (String code : codes) {
                decrementRefCount(code);
            }
        }
    }

    // 재접속 시 KIS 업스트림에 다시 등록해야 할 종목 목록
    public Set<String> activeCodes() {
        synchronized (lock) {
            return new HashSet<>(refCountByCode.keySet());
        }
    }

    int activeCodeCount() {
        synchronized (lock) {
            return refCountByCode.size();
        }
    }

    private void decrementRefCount(String stockCode) {
        int currentRef = refCountByCode.getOrDefault(stockCode, 0);
        if (currentRef <= 1) {
            refCountByCode.remove(stockCode);
            kisWebSocketClient.unsubscribeUpstream(stockCode);
            log.debug("KIS 업스트림 구독 해제 code={}", stockCode);
        } else {
            refCountByCode.put(stockCode, currentRef - 1);
        }
    }
}
