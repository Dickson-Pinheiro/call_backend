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
public class RedisRatingSyncService {
    private static final Logger logger = LoggerFactory.getLogger(RedisRatingSyncService.class);
    private static final String RATING_SYNC_CHANNEL = "rating:sync";

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisRatingSyncService(@Qualifier("customStringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate,
                                ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishRatingAction(String action, Long ratingId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("action", action);
            message.put("ratingId", ratingId);
            message.put("timestamp", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(RATING_SYNC_CHANNEL, json);
        } catch (Exception e) {
            logger.error("Erro ao publicar rating sync: action={}, ratingId={}", action, ratingId, e);
        }
    }

    public static String getChannelName() {
        return RATING_SYNC_CHANNEL;
    }
}
