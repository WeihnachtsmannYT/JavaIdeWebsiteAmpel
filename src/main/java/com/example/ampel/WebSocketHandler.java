package com.example.ampel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;

public class WebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Gson gson = new Gson();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Extract session ID from URL parameters
        String sessionId = extractSessionId(session);

        if (sessionId != null) {
            AmpelSession ampelSession = AmpelSession.getSession(sessionId);
            ampelSession.setSocketSession(session); // 🔄 Update with new connection
            System.out.println("🔄 Re-linked WebSocket for session: " + sessionId);

            // Send confirmation to client
            try {
                session.sendMessage(new TextMessage(
                        objectMapper.writeValueAsString(
                                Map.of("type", "session_relinked", "sessionId", sessionId)
                        )
                ));
            } catch (IOException e) {
                System.err.println("Error sending confirmation: " + e.getMessage());
            }
        }
    }

    private String extractSessionId(WebSocketSession session) {
        // Get session ID from URL query parameters
        URI uri = session.getUri();
        if (uri == null) return null;

        String query = uri.getQuery();
        return Arrays.stream(query.split("&"))
                .filter(param -> param.startsWith("sessionId="))
                .findFirst()
                .map(param -> param.split("=")[1])
                .orElse(null);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonObject json = gson.fromJson(message.getPayload(), JsonObject.class);

        if (!json.has("sessionId")) {
            session.sendMessage(new TextMessage("{\"error\":\"Missing session ID\"}"));
            return;
        }

        String sessionId = json.get("sessionId").getAsString();
        AmpelSession ampelSession = AmpelSession.getSession(sessionId);
        ampelSession.setSocketSession(session);

        // Send confirmation
        JsonObject response = new JsonObject();
        response.addProperty("status", "registered");
        response.addProperty("sessionId", sessionId);
        session.sendMessage(new TextMessage(gson.toJson(response)));

        System.out.println("🔄 WebSocket linked to session: " + sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("❌ WebSocket closed: " + session.getId());
    }
}
