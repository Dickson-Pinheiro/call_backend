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
public class RedisUserSyncService {
    private static final Logger logger = LoggerFactory.getLogger(RedisUserSyncService.class);
    private static final String USER_SYNC_CHANNEL = "user:sync";

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisUserSyncService(@Qualifier("customStringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate,
                                ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishUserAction(String action, Long userId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("action", action);
            message.put("userId", userId);
            message.put("timestamp", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(USER_SYNC_CHANNEL, json);
        } catch (Exception e) {
            logger.error("Erro ao publicar user sync: action={}, userId={}", action, userId, e);
        }
    }

    public static String getChannelName() {
        return USER_SYNC_CHANNEL;
    }
}
