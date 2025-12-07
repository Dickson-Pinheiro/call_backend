package com.group_call.call_backend.websocket.handler;

import com.group_call.call_backend.websocket.message.WebSocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageHandlerFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageHandlerFactory.class);
    
    private final List<MessageHandler> handlers;
    
    public MessageHandlerFactory(List<MessageHandler> handlers) {
        this.handlers = handlers;
    }
    
    public void processMessage(WebSocketMessage message, SimpMessagingTemplate messagingTemplate) {
        for (MessageHandler handler : handlers) {
            if (handler.supports(message)) {
                handler.handleMessage(message, messagingTemplate);
                return;
            }
        }
        
        logger.warn("Nenhum handler encontrado para messageType={}", message.getMessageType());
    }
}
