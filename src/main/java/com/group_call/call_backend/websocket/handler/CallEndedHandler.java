package com.group_call.call_backend.websocket.handler;

import com.group_call.call_backend.websocket.message.WebSocketMessage;
import com.group_call.call_backend.websocket.message.WebSocketMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class CallEndedHandler implements MessageHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CallEndedHandler.class);
    
    @Override
    public void handleMessage(WebSocketMessage message, SimpMessagingTemplate messagingTemplate) {
        try {
            messagingTemplate.convertAndSendToUser(
                message.getTargetUserId().toString(),
                message.getDestination(),
                message.getPayload()
            );
        } catch (Exception e) {
            logger.error("Erro ao processar chamada encerrada: {}", e.getMessage());
        }
    }
    
    @Override
    public boolean supports(WebSocketMessage message) {
        return message.getMessageType() == WebSocketMessageType.CALL_ENDED;
    }
    
    @Override
    public String getHandlerName() {
        return "CallEndedHandler";
    }
}
