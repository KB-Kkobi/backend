package org.kkobi.external.kis.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.kkobi.external.kis.config.KisApiConfig.KisApiProperties;
import org.kkobi.external.kis.realtime.KisApprovalKeyManager;
import org.kkobi.external.kis.realtime.KisTickFrameParser;
import org.kkobi.external.kis.realtime.dto.StockTick;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// KIS 실시간 서비스와 유지하는 업스트림 웹소켓 세션을 관리한다.
// - 등록/해제 프레임을 JSON으로 전송
// - PINGPONG 프레임에 즉시 응답
// - 세션이 끊기면 재접속 후 활성 종목을 다시 등록
@Component
@Log4j2
public class KisWebSocketClient {

    private static final String TR_ID_TICK = KisTickFrameParser.TR_ID;
    private static final long RECONNECT_DELAY_MS = 5_000;

    private final KisApiProperties properties;
    private final KisApprovalKeyManager approvalKeyManager;
    private final KisTickFrameParser tickFrameParser;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Set<String> activeCodes = new CopyOnWriteArraySet<>();
    private final Object sessionLock = new Object();

    private volatile Session upstreamSession;
    private volatile boolean running = true;
    private ScheduledExecutorService reconnectExecutor;

    public KisWebSocketClient(
            KisApiProperties properties,
            KisApprovalKeyManager approvalKeyManager,
            KisTickFrameParser tickFrameParser,
            SimpMessagingTemplate messagingTemplate) {
        this.properties = properties;
        this.approvalKeyManager = approvalKeyManager;
        this.tickFrameParser = tickFrameParser;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    void start() {
        reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "kis-ws-reconnect");
            t.setDaemon(true);
            return t;
        });
        // 최초 연결은 첫 구독 요청이 들어올 때 수행 (필요 시 앞당길 수 있음)
    }

    @PreDestroy
    void stop() {
        running = false;
        if (reconnectExecutor != null) {
            reconnectExecutor.shutdownNow();
        }
        closeSessionQuietly();
    }

    public void subscribeUpstream(String stockCode) {
        activeCodes.add(stockCode);
        ensureConnected();
        sendControlFrame(stockCode, "1");
    }

    public void unsubscribeUpstream(String stockCode) {
        if (!activeCodes.remove(stockCode)) {
            return;
        }
        sendControlFrame(stockCode, "2");
    }

    private void ensureConnected() {
        Session session = upstreamSession;
        if (session != null && session.isOpen()) {
            return;
        }
        synchronized (sessionLock) {
            if (upstreamSession != null && upstreamSession.isOpen()) {
                return;
            }
            connect();
        }
    }

    private void connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI uri = URI.create(properties.wsBaseUrl());
            log.debug("KIS 웹소켓 연결 시도 uri={}", uri);
            upstreamSession = container.connectToServer(new UpstreamEndpoint(),
                    ClientEndpointConfig.Builder.create().build(), uri);
            log.info("KIS 웹소켓 연결 성공 sessionId={}", upstreamSession.getId());
        } catch (Exception ex) {
            log.warn("KIS 웹소켓 연결 실패: {}", ex.getMessage());
            upstreamSession = null;
            scheduleReconnect();
        }
    }

    private void sendControlFrame(String stockCode, String trType) {
        Session session = upstreamSession;
        if (session == null || !session.isOpen()) {
            log.debug("KIS 웹소켓 미연결 상태에서 제어프레임 요청 code={} trType={}", stockCode, trType);
            ensureConnected();
            session = upstreamSession;
            if (session == null || !session.isOpen()) {
                return;
            }
        }
        String payload = buildControlPayload(stockCode, trType);
        try {
            session.getBasicRemote().sendText(payload);
            log.debug("KIS 제어프레임 전송 code={} trType={}", stockCode, trType);
        } catch (IOException ex) {
            log.warn("KIS 제어프레임 전송 실패 code={} trType={} msg={}",
                    stockCode, trType, ex.getMessage());
        }
    }

    private String buildControlPayload(String stockCode, String trType) {
        return "{\"header\":{"
                + "\"approval_key\":\"" + approvalKeyManager.getApprovalKey() + "\","
                + "\"custtype\":\"P\","
                + "\"tr_type\":\"" + trType + "\","
                + "\"content-type\":\"utf-8\""
                + "},\"body\":{\"input\":{"
                + "\"tr_id\":\"" + TR_ID_TICK + "\","
                + "\"tr_key\":\"" + stockCode + "\""
                + "}}}";
    }

    private void handleIncoming(String frame) {
        if (frame == null || frame.isEmpty()) {
            return;
        }
        // 파이프 구분 데이터 프레임(체결 tick)인지 먼저 검사
        if (tickFrameParser.isTickFrame(frame)) {
            List<StockTick> ticks = tickFrameParser.parse(frame);
            for (StockTick tick : ticks) {
                messagingTemplate.convertAndSend("/topic/stocks/" + tick.stockCode(), tick);
            }
            return;
        }
        // JSON 응답 (subscribe 응답, PINGPONG 등)
        try {
            JsonNode json = objectMapper.readTree(frame);
            String trId = json.path("header").path("tr_id").asText("");
            if ("PINGPONG".equals(trId)) {
                Session session = upstreamSession;
                if (session != null && session.isOpen()) {
                    session.getBasicRemote().sendText(frame);
                }
                return;
            }
            String msg = json.path("body").path("msg1").asText("");
            log.debug("KIS 웹소켓 JSON 수신 tr_id={} msg1={}", trId, msg);
        } catch (Exception ex) {
            log.debug("KIS 웹소켓 알 수 없는 프레임 무시 head={}",
                    frame.length() > 60 ? frame.substring(0, 60) : frame);
        }
    }

    private void scheduleReconnect() {
        if (!running || reconnectExecutor == null || reconnectExecutor.isShutdown()) {
            return;
        }
        reconnectExecutor.schedule(this::reconnectWithResubscribe,
                RECONNECT_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void reconnectWithResubscribe() {
        if (!running) {
            return;
        }
        synchronized (sessionLock) {
            connect();
            Session session = upstreamSession;
            if (session == null || !session.isOpen()) {
                return;
            }
            // 활성 종목을 다시 등록
            Set<String> snapshot = new HashSet<>(activeCodes);
            for (String code : snapshot) {
                sendControlFrame(code, "1");
            }
        }
    }

    private void closeSessionQuietly() {
        Session session = upstreamSession;
        if (session == null) {
            return;
        }
        try {
            session.close();
        } catch (IOException ignore) {
        }
        upstreamSession = null;
    }

    private class UpstreamEndpoint extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String frame) {
                    handleIncoming(frame);
                }
            });
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            log.warn("KIS 웹소켓 세션 종료 reason={}", closeReason.getReasonPhrase());
            upstreamSession = null;
            scheduleReconnect();
        }

        @Override
        public void onError(Session session, Throwable ex) {
            log.warn("KIS 웹소켓 오류: {}", ex.getMessage());
        }
    }
}
