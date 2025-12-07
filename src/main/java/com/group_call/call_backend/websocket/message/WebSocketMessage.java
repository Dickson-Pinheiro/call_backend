package com.group_call.call_backend.websocket.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String messageId;
    private WebSocketMessageType messageType;
    private Long targetUserId;
    private String destination;
    private Map<String, Object> payload;
    private LocalDateTime timestamp;
    private String serverId; // Identificar servidor de origem
    
    public WebSocketMessage(WebSocketMessageType messageType, Long targetUserId, String destination, Map<String, Object> payload) {
        this.messageId = UUID.randomUUID().toString();
        this.messageType = messageType;
        this.targetUserId = targetUserId;
        this.destination = destination;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
        this.serverId = getServerIdentifier();
    }
    
    private String getServerIdentifier() {
        // Usa variável de ambiente do Fly.io ou hostname
        String flyMachineId = System.getenv("FLY_MACHINE_ID");
        if (flyMachineId != null && !flyMachineId.isEmpty()) {
            return flyMachineId;
        }
        
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
