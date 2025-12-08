package com.group_call.call_backend.service;

import com.group_call.call_backend.websocket.handler.MessageHandlerFactory;
import com.group_call.call_backend.websocket.message.WebSocketMessage;
import com.group_call.call_backend.websocket.message.WebSocketMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.annotation.PostConstruct;

import java.util.Map;

@Service
public class RedisWebSocketBroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(RedisWebSocketBroadcastService.class);
    private static final String WEBSOCKET_BROADCAST_CHANNEL = "websocket:broadcast";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 100;

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;
    private final MessageHandlerFactory messageHandlerFactory;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;

    public RedisWebSocketBroadcastService(
            @Qualifier("customStringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate,
            RedisMessageListenerContainer redisMessageListenerContainer,
            SimpMessagingTemplate messagingTemplate,
            SimpUserRegistry simpUserRegistry,
            MessageHandlerFactory messageHandlerFactory) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
        this.messagingTemplate = messagingTemplate;
        this.simpUserRegistry = simpUserRegistry;
        this.messageHandlerFactory = messageHandlerFactory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("websocket-retry-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @PostConstruct
    public void init() {
        String currentServerId = getServerIdentifier();
        ChannelTopic topic = new ChannelTopic(WEBSOCKET_BROADCAST_CHANNEL);
        redisMessageListenerContainer.addMessageListener((message, pattern) -> {
                try {
                    String payload = new String(message.getBody());
                    WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);

                    SimpUser simpUser = simpUserRegistry.getUser(wsMessage.getTargetUserId().toString());
                    if (simpUser != null) {
                        messageHandlerFactory.processMessage(wsMessage, messagingTemplate);
                    } else if (currentServerId.equals(wsMessage.getServerId())) {
                        scheduleRetry(wsMessage, MAX_RETRY_ATTEMPTS);
                    }
                } catch (Exception e) {
                    logger.error("Erro ao processar mensagem Redis: {}", e.getMessage());
                }
        }, topic);
    }
    
    private String getServerIdentifier() {
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
    
    private void scheduleRetry(WebSocketMessage message, int attemptsRemaining) {
        if (attemptsRemaining <= 0) {
            logger.warn("Mensagem descartada após {} tentativas - messageId={}, targetUserId={}", 
                       MAX_RETRY_ATTEMPTS, message.getMessageId(), message.getTargetUserId());
            return;
        }
        
        taskScheduler.schedule(() -> {
            SimpUser simpUser = simpUserRegistry.getUser(message.getTargetUserId().toString());
            if (simpUser != null) {
                messageHandlerFactory.processMessage(message, messagingTemplate);
            } else {
                scheduleRetry(message, attemptsRemaining - 1);
            }
        }, java.time.Instant.now().plusMillis(RETRY_DELAY_MS));
    }

    public void broadcastChatMessage(Long targetUserId, Map<String, Object> payload) {
        WebSocketMessage message = new WebSocketMessage(
            WebSocketMessageType.CHAT_MESSAGE, 
            targetUserId, 
            "/queue/chat", 
            payload
        );
        publishToRedis(message);
    }
    
    public void broadcastTypingIndicator(Long targetUserId, Map<String, Object> payload) {
        WebSocketMessage message = new WebSocketMessage(
            WebSocketMessageType.TYPING_INDICATOR, 
            targetUserId, 
            "/queue/typing", 
            payload
        );
        publishToRedis(message);
    }
    
    public void broadcastWebRTCSignal(Long targetUserId, Map<String, Object> payload) {
        WebSocketMessage message = new WebSocketMessage(
            WebSocketMessageType.WEBRTC_SIGNAL, 
            targetUserId, 
            "/queue/webrtc-signal", 
            payload
        );
        publishToRedis(message);
    }
    
    public void broadcastMatchFound(Long targetUserId, Map<String, Object> payload) {
        WebSocketMessage message = new WebSocketMessage(
            WebSocketMessageType.MATCH_FOUND, 
            targetUserId, 
            "/queue/match-found", 
            payload
        );
        publishToRedis(message);
    }
    
    public void broadcastCallEnded(Long targetUserId, Map<String, Object> payload) {
        WebSocketMessage message = new WebSocketMessage(
            WebSocketMessageType.CALL_ENDED, 
            targetUserId, 
            "/queue/call-ended", 
            payload
        );
        publishToRedis(message);
    }
    
    public void broadcastError(Long targetUserId, Map<String, Object> payload) {
        WebSocketMessage message = new WebSocketMessage(
            WebSocketMessageType.ERROR, 
            targetUserId, 
            "/queue/error", 
            payload
        );
        publishToRedis(message);
    }
    
    public void broadcastToUser(Long userId, String destination, Object payload) {
        WebSocketMessageType type = determineMessageType(destination);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> payloadMap = (payload instanceof Map) 
            ? (Map<String, Object>) payload 
            : Map.of("data", payload);
            
        WebSocketMessage message = new WebSocketMessage(type, userId, destination, payloadMap);
        publishToRedis(message);
    }
    
    private WebSocketMessageType determineMessageType(String destination) {
        if (destination.contains("chat")) return WebSocketMessageType.CHAT_MESSAGE;
        if (destination.contains("typing")) return WebSocketMessageType.TYPING_INDICATOR;
        if (destination.contains("webrtc")) return WebSocketMessageType.WEBRTC_SIGNAL;
        if (destination.contains("match")) return WebSocketMessageType.MATCH_FOUND;
        if (destination.contains("call-ended")) return WebSocketMessageType.CALL_ENDED;
        if (destination.contains("error")) return WebSocketMessageType.ERROR;
        return WebSocketMessageType.CHAT_MESSAGE;
    }
    
    private void publishToRedis(WebSocketMessage message) {
        try {
            SimpUser simpUser = simpUserRegistry.getUser(message.getTargetUserId().toString());
            if (simpUser != null && !simpUser.getSessions().isEmpty()) {
                try {
                    messageHandlerFactory.processMessage(message, messagingTemplate);
                    return;
                } catch (Exception e) {
                    logger.warn("Falha na entrega direta, usando Redis - messageId={}", message.getMessageId());
                }
            }
            
            String json = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(WEBSOCKET_BROADCAST_CHANNEL, json);
            
        } catch (JsonProcessingException e) {
            logger.error("Mensagem perdida por falha de serialização - targetUserId={}, error={}", 
                        message.getTargetUserId(), e.getMessage());
        }
    }
}
