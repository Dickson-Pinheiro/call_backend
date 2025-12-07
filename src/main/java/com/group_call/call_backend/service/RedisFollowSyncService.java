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
public class RedisFollowSyncService {

    private static final Logger logger = LoggerFactory.getLogger(RedisFollowSyncService.class);
    private static final String FOLLOW_SYNC_CHANNEL = "follow:sync";

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisFollowSyncService(@Qualifier("customStringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate,
                                  ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishFollowAction(String action, Long followId, Long followerId, Long followingId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("action", action);
            message.put("followId", followId);
            message.put("followerId", followerId);
            message.put("followingId", followingId);
            message.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(FOLLOW_SYNC_CHANNEL, json);
        } catch (Exception e) {
            logger.error("Erro ao publicar follow sync: action={}, followId={}", action, followId, e);
        }
    }

    public static String getChannelName() {
        return FOLLOW_SYNC_CHANNEL;
    }
}
