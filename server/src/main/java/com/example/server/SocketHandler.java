package com.example.server;

import com.example.server.config.WebSocketTextMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class SocketHandler extends TextWebSocketHandler {

    private final AtomicInteger counter = new AtomicInteger(0);
    private final Map<WebSocketSession, Integer> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String LOG = "LOG";
    private static final String SIGNALING = "SIGNALING";

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        WebSocketTextMessage msg = mapper.readValue(message.getPayload(), WebSocketTextMessage.class);
        if (msg.getKind().equals(LOG)) {
            handleLogMessage(session, msg.getData());
        } else if (msg.getKind().equals(SIGNALING)) {
            handleSignalingMessage(session, msg.getData());
        }
    }

    private void handleSignalingMessage(WebSocketSession session, Object data) throws IOException {
        for (WebSocketSession webSocketSession : sessions.keySet()) {
            if (webSocketSession.isOpen() && !session.getId().equals(webSocketSession.getId())) {
                webSocketSession.sendMessage(new TextMessage(mapper.writeValueAsString(data)));
            }
        }
    }

    private void handleLogMessage(WebSocketSession session, Object data) {
        var id = sessions.get(session);
        log.info("Client " + id + " : " + data);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        var id = counter.incrementAndGet();
        log.info("Connection established: " + id);
        sessions.put(session, id);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        var id = sessions.get(session);
        log.info("Connection closed: " + id);
        sessions.remove(session);
    }
}
