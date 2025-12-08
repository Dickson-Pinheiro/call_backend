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
public class RedisCallSyncListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RedisCallSyncListener.class);
    private final CallService callService;
    private final ObjectMapper objectMapper;

    public RedisCallSyncListener(@Lazy CallService callService, ObjectMapper objectMapper) {
        this.callService = callService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            String action = (String) data.get("action");
            Long callId = ((Number) data.get("callId")).longValue();
            callService.syncCallAction(action, callId);
        } catch (Exception e) {
            logger.error("Erro ao processar mensagem de call sync", e);
        }
    }
}
