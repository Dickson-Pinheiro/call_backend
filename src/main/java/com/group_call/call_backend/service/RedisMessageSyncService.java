package com.group_call.call_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RedisMessageSyncService {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageSyncService.class);
    private static final String MESSAGE_SYNC_CHANNEL = "message:sync";

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisMessageSyncService(@Qualifier("customStringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate,
                                ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishMessageAction(String action, Long messageId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("action", action);
            message.put("messageId", messageId);
            message.put("timestamp", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(MESSAGE_SYNC_CHANNEL, json);
        } catch (Exception e) {
            logger.error("Erro ao publicar message sync: action={}, messageId={}", action, messageId, e);
        }
    }

    public static String getChannelName() {
        return MESSAGE_SYNC_CHANNEL;
    }
}
