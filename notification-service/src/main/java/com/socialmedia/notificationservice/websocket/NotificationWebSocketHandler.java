package com.socialmedia.notificationservice.websocket;

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

/**
 * Tracks one live WebSocket session per connected user (keyed by userId,
 * taken from the URL template /ws/notifications/{userId}) and exposes
 * sendToUser() so NotificationService can push real-time events - matching
 * the "Send real-time notification via WebSocket" pattern from the
 * project's sample code.
 */
@Component
@Slf4j
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        sessions.put(userId, session);
        log.info("Notification WS connected for user {}", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(extractUserId(session));
    }

    public void sendToUser(Long userId, Object payload) {
        WebSocketSession session = sessions.get(String.valueOf(userId));
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            } catch (IOException e) {
                log.warn("Failed to push notification to user {}: {}", userId, e.getMessage());
            }
        }
    }

    private String extractUserId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }
}
