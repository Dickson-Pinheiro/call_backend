package com.group_call.call_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Service
public class RedisWebSocketBroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(RedisWebSocketBroadcastService.class);
    private static final String WEBSOCKET_BROADCAST_CHANNEL = "websocket:broadcast";

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RedisWebSocketBroadcastService(
            @Qualifier("customStringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate,
            RedisMessageListenerContainer redisMessageListenerContainer,
            SimpMessagingTemplate messagingTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        // Subscribe to Redis channel for WebSocket broadcasts
        ChannelTopic topic = new ChannelTopic(WEBSOCKET_BROADCAST_CHANNEL);
        redisMessageListenerContainer.addMessageListener((message, pattern) -> {
            try {
                String payload = new String(message.getBody());
                WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
                
                logger.info(">>> [REDIS_WS_BROADCAST] Recebida mensagem Redis - userId={}, destination={}", 
                           wsMessage.getUserId(), wsMessage.getDestination());
                
                // Envia para o usuário conectado neste servidor
                messagingTemplate.convertAndSendToUser(
                    wsMessage.getUserId().toString(), 
                    wsMessage.getDestination(), 
                    wsMessage.getPayload()
                );
                
            } catch (Exception e) {
                logger.error(">>> [REDIS_WS_BROADCAST] Erro ao processar mensagem Redis", e);
            }
        }, topic);
        
        logger.info(">>> [REDIS_WS_BROADCAST] Listener registrado no canal: {}", WEBSOCKET_BROADCAST_CHANNEL);
    }

    /**
     * Publica mensagem WebSocket para todos os servidores via Redis
     */
    public void broadcastToUser(Long userId, String destination, Object payload) {
        try {
            WebSocketMessage message = new WebSocketMessage(userId, destination, payload);
            String json = objectMapper.writeValueAsString(message);
            
            logger.info(">>> [REDIS_WS_BROADCAST] Publicando mensagem - userId={}, destination={}", 
                       userId, destination);
            
            stringRedisTemplate.convertAndSend(WEBSOCKET_BROADCAST_CHANNEL, json);
            
        } catch (JsonProcessingException e) {
            logger.error(">>> [REDIS_WS_BROADCAST] Erro ao serializar mensagem", e);
        }
    }

    /**
     * Classe interna para representar mensagem WebSocket
     */
    public static class WebSocketMessage {
        private Long userId;
        private String destination;
        private Object payload;

        public WebSocketMessage() {}

        public WebSocketMessage(Long userId, String destination, Object payload) {
            this.userId = userId;
            this.destination = destination;
            this.payload = payload;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public Object getPayload() {
            return payload;
        }

        public void setPayload(Object payload) {
            this.payload = payload;
        }
    }
}
