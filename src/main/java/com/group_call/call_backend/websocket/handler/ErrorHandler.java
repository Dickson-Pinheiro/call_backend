package com.group_call.call_backend.websocket.handler;

import com.group_call.call_backend.websocket.message.WebSocketMessage;
import com.group_call.call_backend.websocket.message.WebSocketMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ErrorHandler implements MessageHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    
    @Override
    public void handleMessage(WebSocketMessage message, SimpMessagingTemplate messagingTemplate) {
        try {
            messagingTemplate.convertAndSendToUser(
                message.getTargetUserId().toString(),
                message.getDestination(),
                message.getPayload()
            );
        } catch (Exception e) {
            logger.error("Erro ao processar mensagem de erro: {}", e.getMessage());
        }
    }
    
    @Override
    public boolean supports(WebSocketMessage message) {
        return message.getMessageType() == WebSocketMessageType.ERROR;
    }
    
    @Override
    public String getHandlerName() {
        return "ErrorHandler";
    }
}
