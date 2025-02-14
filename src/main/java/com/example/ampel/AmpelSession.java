package com.example.ampel;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

public class AmpelSession {
    private static final ConcurrentHashMap<String, AmpelSession> SESSIONS = new ConcurrentHashMap<>();

    private final String sessionId;
    private WebSocketSession socketSession;
    private final AmpelController ampelController;

    public AmpelSession(String sessionId) {
        this.sessionId = sessionId;
        this.ampelController = new AmpelController(this);
        System.out.println("🆕 Created new AmpelSession for session ID: " + sessionId);
    }

    public static AmpelSession getSession(String sessionId) {
        AmpelSession session = SESSIONS.computeIfAbsent(sessionId, id -> new AmpelSession(id));
        System.out.println("🔍 Retrieved AmpelSession for session ID: " + sessionId);
        return session;
    }

    public static void removeSession(String sessionId) {
        SESSIONS.remove(sessionId);
        System.out.println("❌ Removed AmpelSession for session ID: " + sessionId);
    }

    public void setSocketSession(WebSocketSession newSession) {
        if (this.socketSession != null && this.socketSession.isOpen()) {
            try {
                this.socketSession.close(CloseStatus.NORMAL); // Close old connection
            } catch (IOException e) {
                System.err.println("⚠️ Error closing old session: " + e.getMessage());
            }
        }
        this.socketSession = newSession;
        System.out.println("🔄 Updated WebSocket for session: " + sessionId);
    }

    public WebSocketSession getSocketSession() {
        return socketSession;
    }

    public AmpelController getAmpelController() {
        return ampelController;
    }
}
