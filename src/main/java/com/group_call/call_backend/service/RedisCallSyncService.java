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
public class RedisCallSyncService {
    private static final Logger logger = LoggerFactory.getLogger(RedisCallSyncService.class);
    private static final String CALL_SYNC_CHANNEL = "call:sync";

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCallSyncService(@Qualifier("customStringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate,
                                ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishCallAction(String action, Long callId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("action", action);
            message.put("callId", callId);
            message.put("timestamp", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(CALL_SYNC_CHANNEL, json);
        } catch (Exception e) {
            logger.error("Erro ao publicar call sync: action={}, callId={}", action, callId, e);
        }
    }

    public static String getChannelName() {
        return CALL_SYNC_CHANNEL;
    }
}
