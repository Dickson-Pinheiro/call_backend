package com.group_call.call_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisMatchmakingService {

    private final RedisTemplate<String, Long> redisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;

    private static final String QUEUE_KEY = "matchmaking:queue";
    private static final String USER_IN_CALL_PREFIX = "matchmaking:in_call:";
    private static final String USER_SESSION_PREFIX = "matchmaking:session:";
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    @Autowired
    public RedisMatchmakingService(RedisTemplate<String, Long> redisTemplate,
                                   @Qualifier("customStringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void joinQueue(Long userId) {
        redisTemplate.opsForList().rightPush(QUEUE_KEY, userId);
    }

    public void leaveQueue(Long userId) {
        redisTemplate.opsForList().remove(QUEUE_KEY, 1, userId);
    }

    public Optional<Long[]> tryMatch() {
        Long user1 = redisTemplate.opsForList().leftPop(QUEUE_KEY);
        Long user2 = redisTemplate.opsForList().leftPop(QUEUE_KEY);

        if (user1 != null && user2 != null) {
            return Optional.of(new Long[]{user1, user2});
        }

        if (user1 != null) {
            redisTemplate.opsForList().leftPush(QUEUE_KEY, user1);
        }

        return Optional.empty();
    }

    public Long getQueueSize() {
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        return size != null ? size : 0L;
    }

    public List<Long> getQueueUsers() {
        Long size = getQueueSize();
        if (size == 0) {
            return List.of();
        }
        return redisTemplate.opsForList().range(QUEUE_KEY, 0, -1);
    }

    public void setUserInCall(Long userId, Long partnerId) {
        String key = USER_IN_CALL_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(key, partnerId.toString(), SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    public void removeUserFromCall(Long userId) {
        String key = USER_IN_CALL_PREFIX + userId;
        stringRedisTemplate.delete(key);
    }

    public boolean isUserInCall(Long userId) {
        String key = USER_IN_CALL_PREFIX + userId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    public Optional<Long> getPartnerUserId(Long userId) {
        String key = USER_IN_CALL_PREFIX + userId;
        String partnerIdStr = stringRedisTemplate.opsForValue().get(key);
        if (partnerIdStr != null) {
            try {
                return Optional.of(Long.parseLong(partnerIdStr));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public void saveUserSession(Long userId, String sessionId) {
        String key = USER_SESSION_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(key, sessionId, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    public void removeUserSession(Long userId) {
        String key = USER_SESSION_PREFIX + userId;
        stringRedisTemplate.delete(key);
    }

    public Optional<String> getUserSession(Long userId) {
        String key = USER_SESSION_PREFIX + userId;
        String sessionId = stringRedisTemplate.opsForValue().get(key);
        return Optional.ofNullable(sessionId);
    }

    public void clearQueue() {
        redisTemplate.delete(QUEUE_KEY);
    }

    public void clearAllMatchmakingData() {
        redisTemplate.delete(QUEUE_KEY);
        
        Set<String> inCallKeys = stringRedisTemplate.keys(USER_IN_CALL_PREFIX + "*");
        if (inCallKeys != null && !inCallKeys.isEmpty()) {
            stringRedisTemplate.delete(inCallKeys);
        }
        
        Set<String> sessionKeys = stringRedisTemplate.keys(USER_SESSION_PREFIX + "*");
        if (sessionKeys != null && !sessionKeys.isEmpty()) {
            stringRedisTemplate.delete(sessionKeys);
        }
        
    }
}
