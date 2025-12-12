package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.CallRepository;
import com.group_call.call_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class MatchmakingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchmakingService.class);

    private final UserRepository userRepository;
    private final CallRepository callRepository;
    private final RedisMatchmakingService redisMatchmaking;
    private final RedisWebSocketBroadcastService redisBroadcast;

    private final Object matchLock = new Object();

    @Autowired
    public MatchmakingService(UserRepository userRepository, CallRepository callRepository,
            RedisMatchmakingService redisMatchmaking,
            RedisWebSocketBroadcastService redisBroadcast) {
        this.userRepository = userRepository;
        this.callRepository = callRepository;
        this.redisMatchmaking = redisMatchmaking;
        this.redisBroadcast = redisBroadcast;
    }

    public void registerSession(Long userId, String sessionId) {
        redisMatchmaking.saveUserSession(userId, sessionId);
    }

    public void unregisterSession(Long userId) {
        redisMatchmaking.removeUserSession(userId);
        redisMatchmaking.leaveQueue(userId);
    }

    public void joinQueue(Long userId) {
        if (redisMatchmaking.isUserInCall(userId)) {
            boolean hasActiveCall = hasActiveCallInDatabase(userId);

            if (hasActiveCall) {
                throw new IllegalStateException("Usuário já está em uma chamada");
            } else {
                redisMatchmaking.removeUserFromCall(userId);

                Optional<Long> partnerIdOpt = redisMatchmaking.getPartnerUserId(userId);
                if (partnerIdOpt.isPresent()) {
                    redisMatchmaking.removeUserFromCall(partnerIdOpt.get());
                }
            }
        }

        redisMatchmaking.joinQueue(userId);
        tryMatch();
    }

    private boolean hasActiveCallInDatabase(Long userId) {
        try {
            // Ideally should be a custom query in repository:
            // findByStatusAndUser1IdOrUser2Id
            java.util.List<CallEntity> activeCalls = callRepository.findByStatus(CallEntity.CallStatus.ACTIVE);
            for (CallEntity call : activeCalls) {
                if (call.getUser1().getId().equals(userId) || call.getUser2().getId().equals(userId)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Erro ao verificar chamadas ativas para userId={}", userId, e);
            return false;
        }
    }

    public void leaveQueue(Long userId) {
        redisMatchmaking.leaveQueue(userId);
    }

    public void cleanupUserOnDisconnect(Long userId) {
        try {
            redisMatchmaking.leaveQueue(userId);
            endActiveCallForUser(userId);
            redisMatchmaking.removeUserSession(userId);

            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setIsOnline(false);
                userRepository.save(user);
            }
        } catch (Exception e) {
            logger.error("Erro ao limpar estado do usuário userId={}", userId, e);
        }
    }

    private void endActiveCallForUser(Long userId) {
        try {
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return;
            }

            java.util.List<CallEntity> activeCalls = callRepository.findByStatus(CallEntity.CallStatus.ACTIVE);
            CallEntity activeCall = null;

            for (CallEntity call : activeCalls) {
                if (call.getUser1().getId().equals(userId) || call.getUser2().getId().equals(userId)) {
                    activeCall = call;
                    break;
                }
            }

            if (activeCall == null) {
                return;
            }

            Long partnerId = activeCall.getUser1().getId().equals(userId)
                    ? activeCall.getUser2().getId()
                    : activeCall.getUser1().getId();

            activeCall.setStatus(CallEntity.CallStatus.COMPLETED);
            activeCall.setEndedAt(LocalDateTime.now());

            if (activeCall.getStartedAt() != null) {
                long duration = java.time.Duration.between(activeCall.getStartedAt(), activeCall.getEndedAt())
                        .getSeconds();
                activeCall.setDurationSeconds((int) duration);
            }

            callRepository.save(activeCall);

            redisMatchmaking.removeUserFromCall(userId);
            redisMatchmaking.removeUserFromCall(partnerId);

            Map<String, Object> notification = Map.of(
                    "callId", activeCall.getId(),
                    "reason", "partner_disconnected",
                    "partnerId", userId);

            sendCallEnded(partnerId, notification);
        } catch (Exception e) {
            logger.error("Erro ao encerrar chamada ativa para userId={}", userId, e);
        }
    }

    public void nextPerson(Long userId) {
        try {
            Optional<Long> partnerIdOpt = redisMatchmaking.getPartnerUserId(userId);

            if (partnerIdOpt.isPresent()) {
                Long partnerId = partnerIdOpt.get();

                redisMatchmaking.removeUserFromCall(userId);
                redisMatchmaking.removeUserFromCall(partnerId);

                Map<String, Object> notification = Map.of(
                        "reason", "partner_next_person",
                        "partnerId", userId);
                sendCallEnded(partnerId, notification);
            }

            joinQueue(userId);
        } catch (Exception e) {
            logger.error("Erro ao processar próxima pessoa para userId={}", userId, e);
            throw e;
        }
    }

    public void endCall(Long callId, Long userId) {
        CallEntity call = callRepository.findById(callId).orElse(null);
        if (call == null) {
            return;
        }

        call.setStatus(CallEntity.CallStatus.COMPLETED);
        call.setEndedAt(LocalDateTime.now());

        if (call.getStartedAt() != null) {
            long duration = java.time.Duration.between(call.getStartedAt(), call.getEndedAt()).getSeconds();
            call.setDurationSeconds((int) duration);
        }

        callRepository.save(call);

        Long user1Id = call.getUser1().getId();
        Long user2Id = call.getUser2().getId();

        redisMatchmaking.removeUserFromCall(user1Id);
        redisMatchmaking.removeUserFromCall(user2Id);

        updateUserStatusAfterCall(user1Id);
        updateUserStatusAfterCall(user2Id);

        Map<String, Object> notification = Map.of(
                "callId", callId,
                "reason", "call_ended");

        sendCallEnded(user1Id, notification);
        sendCallEnded(user2Id, notification);
    }

    private void updateUserStatusAfterCall(Long userId) {
        try {
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                userRepository.save(user);
            }
        } catch (Exception e) {
            logger.error("Erro ao atualizar usuário userId={}", userId, e);
        }
    }

    private void tryMatch() {
        synchronized (matchLock) {
            Long queueSize = redisMatchmaking.getQueueSize();

            if (queueSize < 2) {
                return;
            }

            Optional<Long[]> matchResult = redisMatchmaking.tryMatch();

            if (matchResult.isEmpty()) {
                return;
            }

            Long[] users = matchResult.get();
            Long user1Id = users[0];
            Long user2Id = users[1];

            if (user1Id.equals(user2Id)) {
                logger.error("Erro crítico: mesmo usuário pareado duas vezes - userId={}", user1Id);
                redisMatchmaking.joinQueue(user1Id);
                return;
            }

            Optional<UserEntity> user1Opt = userRepository.findById(user1Id);
            Optional<UserEntity> user2Opt = userRepository.findById(user2Id);

            if (user1Opt.isEmpty() || user2Opt.isEmpty()) {
                if (user1Opt.isPresent())
                    redisMatchmaking.joinQueue(user1Id);
                if (user2Opt.isPresent())
                    redisMatchmaking.joinQueue(user2Id);
                return;
            }

            UserEntity user1 = user1Opt.get();
            UserEntity user2 = user2Opt.get();

            CallEntity call = new CallEntity();
            call.setUser1(user1);
            call.setUser2(user2);
            call.setStartedAt(LocalDateTime.now());
            call.setCallType(CallEntity.CallType.VIDEO);
            call.setStatus(CallEntity.CallStatus.ACTIVE);

            call = callRepository.save(call);

            redisMatchmaking.setUserInCall(user1Id, user2Id);
            redisMatchmaking.setUserInCall(user2Id, user1Id);

            Map<String, Object> matchData = Map.of(
                    "callId", call.getId(),
                    "peerId", user2Id,
                    "peerName", user2.getName());
            sendMatchFound(user1Id, matchData);

            matchData = Map.of(
                    "callId", call.getId(),
                    "peerId", user1Id,
                    "peerName", user1.getName());
            sendMatchFound(user2Id, matchData);
        }
    }

    private void sendMatchFound(Long userId, Map<String, Object> matchData) {
        redisBroadcast.broadcastMatchFound(userId, matchData);
    }

    private void sendCallEnded(Long userId, Map<String, Object> endData) {
        redisBroadcast.broadcastCallEnded(userId, endData);
    }

    public Long getUserCallId(Long userId) {
        if (redisMatchmaking.isUserInCall(userId)) {
            return -1L;
        }
        return null;
    }

    public boolean isInCall(Long userId) {
        return redisMatchmaking.isUserInCall(userId);
    }

    public void forceCleanupCallState(Long userId) {
        try {
            redisMatchmaking.removeUserFromCall(userId);

            Optional<Long> partnerIdOpt = redisMatchmaking.getPartnerUserId(userId);
            if (partnerIdOpt.isPresent()) {
                Long partnerId = partnerIdOpt.get();
                redisMatchmaking.removeUserFromCall(partnerId);
            }
        } catch (Exception e) {
            logger.error("Erro ao forçar limpeza de estado para userId={}", userId, e);
        }
    }
}
