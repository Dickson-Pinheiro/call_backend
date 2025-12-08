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
public class RedisUserSyncListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RedisUserSyncListener.class);
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public RedisUserSyncListener(@Lazy UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            String action = (String) data.get("action");
            Long userId = ((Number) data.get("userId")).longValue();
            userService.syncUserAction(action, userId);
        } catch (Exception e) {
            logger.error("Erro ao processar mensagem de user sync", e);
        }
    }
}
