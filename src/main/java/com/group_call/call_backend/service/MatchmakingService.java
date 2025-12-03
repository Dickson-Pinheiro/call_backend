package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.UserRepository;
import com.group_call.call_backend.tree.CallTree;
import com.group_call.call_backend.tree.UserTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class MatchmakingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchmakingService.class);

    private final UserTree userTree;
    private final UserRepository userRepository;
    private final CallTree callTree;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisMatchmakingService redisMatchmaking;
    private final RedisWebSocketBroadcastService redisBroadcast;

    private final Object matchLock = new Object();

    @Autowired
    public MatchmakingService(UserTree userTree, UserRepository userRepository, CallTree callTree, 
                             SimpMessagingTemplate messagingTemplate,
                             RedisMatchmakingService redisMatchmaking,
                             RedisWebSocketBroadcastService redisBroadcast) {
        this.userTree = userTree;
        this.userRepository = userRepository;
        this.callTree = callTree;
        this.messagingTemplate = messagingTemplate;
        this.redisMatchmaking = redisMatchmaking;
        this.redisBroadcast = redisBroadcast;
    }
    
    public void registerSession(Long userId, String sessionId) {
        redisMatchmaking.saveUserSession(userId, sessionId);
        logger.info("Sessão registrada para userId={}, sessionId={}", userId, sessionId);
    }

    public void unregisterSession(Long userId) {
        redisMatchmaking.removeUserSession(userId);
        redisMatchmaking.leaveQueue(userId);
        logger.info("Sessão removida para userId={}", userId);
    }

    public void joinQueue(Long userId) {
        logger.info(">>> [JOIN_QUEUE] Tentativa de entrada - userId={}", userId);
        
        if (redisMatchmaking.isUserInCall(userId)) {
            logger.warn(">>> [JOIN_QUEUE] Usuário já está em chamada - userId={}", userId);
            throw new IllegalStateException("Usuário já está em uma chamada");
        }

        redisMatchmaking.joinQueue(userId);
        logger.info(">>> [JOIN_QUEUE] Usuário adicionado à fila Redis - userId={}, Total na fila: {}", 
                    userId, redisMatchmaking.getQueueSize());
        tryMatch();
    }
    
    public void leaveQueue(Long userId) {
        redisMatchmaking.leaveQueue(userId);
        logger.info("Usuário {} saiu da fila", userId);
    }
    
    public void cleanupUserOnDisconnect(Long userId) {
        logger.info(">>> [CLEANUP_DISCONNECT] Limpando estado do usuário - userId={}", userId);
        
        try {
            redisMatchmaking.leaveQueue(userId);
            logger.info(">>> [CLEANUP_DISCONNECT] Usuário removido da fila - userId={}", userId);
            
            redisMatchmaking.removeUserFromCall(userId);
            logger.info(">>> [CLEANUP_DISCONNECT] Usuário removido do estado 'em chamada' - userId={}", userId);
            
            redisMatchmaking.removeUserSession(userId);
            logger.info(">>> [CLEANUP_DISCONNECT] Sessão WebSocket removida - userId={}", userId);
            
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setIsOnline(false);
                userRepository.save(user);
                userTree.updateUser(user);
                logger.info(">>> [CLEANUP_DISCONNECT] Usuário atualizado como offline - userId={}", userId);
            }
            
        } catch (Exception e) {
            logger.error(">>> [CLEANUP_DISCONNECT] Erro ao limpar estado do usuário - userId={}", userId, e);
        }
    }
    
    public void nextPerson(Long userId) {
        Optional<String> partnerIdStr = redisMatchmaking.getUserSession(userId);
        if (partnerIdStr.isPresent()) {
            redisMatchmaking.removeUserFromCall(userId);
        }
        joinQueue(userId);
    }
    
    public void endCall(Long callId, Long userId) {
        CallEntity call = callTree.findById(callId);
        if (call == null) {
            return;
        }
        call.setStatus(CallEntity.CallStatus.COMPLETED);
        call.setEndedAt(LocalDateTime.now());
        
        if (call.getStartedAt() != null) {
            long duration = java.time.Duration.between(call.getStartedAt(), call.getEndedAt()).getSeconds();
            call.setDurationSeconds((int) duration);
        }

        callTree.updateCall(call);
        Long user1Id = call.getUser1().getId();
        Long user2Id = call.getUser2().getId();
        
        redisMatchmaking.removeUserFromCall(user1Id);
        redisMatchmaking.removeUserFromCall(user2Id);
        
        updateUserStatusAfterCall(user1Id);
        updateUserStatusAfterCall(user2Id);

        sendToUser(user1Id, "/queue/call-ended", Map.of("callId", callId));
        sendToUser(user2Id, "/queue/call-ended", Map.of("callId", callId));

        logger.info("Chamada {} encerrada", callId);
    }
    
    private void updateUserStatusAfterCall(Long userId) {
        try {
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                userTree.updateUser(user);
                logger.info(">>> [END_CALL] Usuário atualizado na árvore - userId={}", userId);
            }
        } catch (Exception e) {
            logger.error(">>> [END_CALL] Erro ao atualizar usuário - userId={}", userId, e);
        }
    }

    private void tryMatch() {
        synchronized (matchLock) {
            Long queueSize = redisMatchmaking.getQueueSize();
            logger.info(">>> [TRY_MATCH] Iniciando tentativa de pareamento - Fila: {}", queueSize);
            
            if (queueSize < 2) {
                logger.info(">>> [TRY_MATCH] Fila insuficiente - size={}", queueSize);
                return;
            }

            Optional<Long[]> matchResult = redisMatchmaking.tryMatch();
            
            if (matchResult.isEmpty()) {
                logger.info(">>> [TRY_MATCH] Nenhum match encontrado");
                return;
            }

            Long[] users = matchResult.get();
            Long user1Id = users[0];
            Long user2Id = users[1];

            logger.info(">>> [TRY_MATCH] Usuários retirados da fila Redis - user1Id={}, user2Id={}", user1Id, user2Id);

            if (user1Id.equals(user2Id)) {
                logger.error(">>> [TRY_MATCH] ERRO CRÍTICO: Mesmo usuário duas vezes! userId={}", user1Id);
                redisMatchmaking.joinQueue(user1Id);
                return;
            }

            Optional<UserEntity> user1Opt = userRepository.findById(user1Id);
            Optional<UserEntity> user2Opt = userRepository.findById(user2Id);

            if (user1Opt.isEmpty() || user2Opt.isEmpty()) {
                logger.error(">>> [TRY_MATCH] Usuário não encontrado no banco - user1Id={}, user2Id={}, user1Present={}, user2Present={}", 
                            user1Id, user2Id, user1Opt.isPresent(), user2Opt.isPresent());
                if (user1Opt.isPresent()) redisMatchmaking.joinQueue(user1Id);
                if (user2Opt.isPresent()) redisMatchmaking.joinQueue(user2Id);
                return;
            }

            UserEntity user1 = user1Opt.get();
            UserEntity user2 = user2Opt.get();

            logger.info(">>> [TRY_MATCH] Criando chamada - user1: {} ({}), user2: {} ({})", 
                        user1Id, user1.getName(), user2Id, user2.getName());

            CallEntity call = new CallEntity();
            call.setUser1(user1);
            call.setUser2(user2);
            call.setStartedAt(LocalDateTime.now());
            call.setCallType(CallEntity.CallType.VIDEO);
            call.setStatus(CallEntity.CallStatus.ACTIVE);

            call = callTree.addCall(call);

            redisMatchmaking.setUserInCall(user1Id, user2Id);
            redisMatchmaking.setUserInCall(user2Id, user1Id);

            Map<String, Object> matchData = Map.of(
                "callId", call.getId(),
                "peerId", user2Id,
                "peerName", user2.getName()
            );
            sendToUser(user1Id, "/queue/match-found", matchData);

            matchData = Map.of(
                "callId", call.getId(),
                "peerId", user1Id,
                "peerName", user1.getName()
            );
            sendToUser(user2Id, "/queue/match-found", matchData);

            logger.info(">>> [TRY_MATCH] Pareamento concluído! CallID={}, User1={} ({}), User2={} ({})", 
                        call.getId(), user1Id, user1.getName(), user2Id, user2.getName());
        }
    }

    private void sendToUser(Long userId, String destination, Object payload) {
        // Envia localmente para usuários conectados a este servidor
        messagingTemplate.convertAndSendToUser(userId.toString(), destination, payload);
        
        // Broadcast via Redis para outros servidores
        redisBroadcast.broadcastToUser(userId, destination, payload);
        
        logger.info(">>> [SEND_TO_USER] Mensagem enviada (local + Redis) - userId={}, destination={}", userId, destination);
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
}
