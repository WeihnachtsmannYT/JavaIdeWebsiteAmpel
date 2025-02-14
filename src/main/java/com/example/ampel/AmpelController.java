package com.example.ampel;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AmpelController {
    private final AmpelSession ampelSession; // Hält die Referenz zur Session
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AmpelController(AmpelSession ampelSession) {
        this.ampelSession = ampelSession; // 💉 Dependency Injection
    }

    public void set(int index, int r, int g, int b) {
        WebSocketSession currentSession = ampelSession.getSocketSession();
        System.out.println("AmpelController.set() called from session" + currentSession);
        if (currentSession != null && currentSession.isOpen()) {
            try {
                Map<String, Object> message = new HashMap<>();
                message.put("index", index);
                message.put("r", r);
                message.put("g", g);
                message.put("b", b);

                String jsonMessage = objectMapper.writeValueAsString(message);
                System.out.println("📤 Sende JSON an Frontend: " + jsonMessage);

                currentSession.sendMessage(new TextMessage(jsonMessage));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("❌ WebSocket nicht verbunden oder geschlossen.");
        }
    }

}
