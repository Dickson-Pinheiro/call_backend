package com.group_call.call_backend.websocket;

import com.group_call.call_backend.service.MatchmakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final MatchmakingService matchmakingService;

    public WebSocketEventListener(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        String userIdStr = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;
        
        if (userIdStr != null) {
            Long userId = Long.parseLong(userIdStr);
            
            
            // Limpa todo o estado do usuário (fila, chamada, sessão, status)
            matchmakingService.cleanupUserOnDisconnect(userId);
        }
    }
}
