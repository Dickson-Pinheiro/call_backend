package com.group_call.call_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RedisFollowSyncListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisFollowSyncListener.class);

    private final FollowService followService;
    private final ObjectMapper objectMapper;

    public RedisFollowSyncListener(@Lazy FollowService followService, ObjectMapper objectMapper) {
        this.followService = followService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(json, Map.class);

            String action = (String) data.get("action");
            Long followId = ((Number) data.get("followId")).longValue();
            Long followerId = ((Number) data.get("followerId")).longValue();
            Long followingId = ((Number) data.get("followingId")).longValue();

            followService.syncFollowAction(action, followId, followerId, followingId);
        } catch (Exception e) {
            logger.error("Erro ao processar mensagem de follow sync", e);
        }
    }
}
