package com.group_call.call_backend.websocket.handler;

import com.group_call.call_backend.websocket.message.WebSocketMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public interface MessageHandler {
    /**
     * Processa a mensagem recebida do Redis e entrega ao usuário local
     */
    void handleMessage(WebSocketMessage message, SimpMessagingTemplate messagingTemplate);
    
    /**
     * Verifica se este handler suporta o tipo de mensagem
     */
    boolean supports(WebSocketMessage message);
    
    /**
     * Nome do handler para logs
     */
    String getHandlerName();
}
