package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.tree.CallTree;
import com.group_call.call_backend.tree.UserTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MatchmakingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchmakingService.class);

    private final UserTree userTree;
    private final CallTree callTree;
    private final SimpMessagingTemplate messagingTemplate;

    private final Queue<Long> waitingQueue = new ConcurrentLinkedQueue<>();
    
    private final Map<Long, Long> userInCall = new ConcurrentHashMap<>();
    
    private final Map<Long, String> userSessions = new ConcurrentHashMap<>();
    
    // Lock para sincronizar o método tryMatch
    private final Object matchLock = new Object();

    public MatchmakingService(UserTree userTree, CallTree callTree, SimpMessagingTemplate messagingTemplate) {
        this.userTree = userTree;
        this.callTree = callTree;
        this.messagingTemplate = messagingTemplate;
    }
    public void registerSession(Long userId, String sessionId) {
        userSessions.put(userId, sessionId);
        logger.info("Sessão registrada para userId={}, sessionId={}", userId, sessionId);
    }

    public void unregisterSession(Long userId) {
        userSessions.remove(userId);
        waitingQueue.remove(userId);
        logger.info("Sessão removida para userId={}", userId);
    }

    public void joinQueue(Long userId) {
        logger.info(">>> [JOIN_QUEUE] Tentativa de entrada - userId={}", userId);
        
        if (userInCall.containsKey(userId)) {
            logger.warn(">>> [JOIN_QUEUE] Usuário já está em chamada - userId={}", userId);
            throw new IllegalStateException("Usuário já está em uma chamada");
        }

        if (waitingQueue.contains(userId)) {
            logger.warn(">>> [JOIN_QUEUE] Usuário já está na fila - userId={}", userId);
            return;
        }

        waitingQueue.offer(userId);
        logger.info(">>> [JOIN_QUEUE] Usuário adicionado à fila - userId={}, Total na fila: {}, Fila atual: {}", 
                    userId, waitingQueue.size(), waitingQueue);
        tryMatch();
    }
    public void leaveQueue(Long userId) {
        waitingQueue.remove(userId);
        logger.info("Usuário {} saiu da fila", userId);
    }
    public void nextPerson(Long userId) {
        Long callId = userInCall.get(userId);
        if (callId != null) {
            endCall(callId, userId);
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
        
        userInCall.remove(user1Id);
        userInCall.remove(user2Id);

        sendToUser(user1Id, "/queue/call-ended", Map.of("callId", callId));
        sendToUser(user2Id, "/queue/call-ended", Map.of("callId", callId));

        logger.info("Chamada {} encerrada", callId);
    }

    private void tryMatch() {
        synchronized (matchLock) {
            logger.info(">>> [TRY_MATCH] Iniciando tentativa de pareamento - Fila: {}", waitingQueue.size());
            
            if (waitingQueue.size() < 2) {
                logger.info(">>> [TRY_MATCH] Fila insuficiente - size={}", waitingQueue.size());
                return;
            }

            Long user1Id = waitingQueue.poll();
            Long user2Id = waitingQueue.poll();

            logger.info(">>> [TRY_MATCH] Usuários retirados da fila - user1Id={}, user2Id={}", user1Id, user2Id);

            if (user1Id == null || user2Id == null) {
                logger.error(">>> [TRY_MATCH] Erro: usuário null - user1Id={}, user2Id={}", user1Id, user2Id);
                return;
            }
            
            if (user1Id.equals(user2Id)) {
                logger.error(">>> [TRY_MATCH] ERRO CRÍTICO: Mesmo usuário duas vezes! userId={}", user1Id);
                // Recolocar na fila
                waitingQueue.offer(user1Id);
                return;
            }

            UserEntity user1 = userTree.findById(user1Id);
            UserEntity user2 = userTree.findById(user2Id);

            if (user1 == null || user2 == null) {
                logger.error(">>> [TRY_MATCH] Usuário não encontrado no banco - user1={}, user2={}", user1, user2);
                return;
            }

            logger.info(">>> [TRY_MATCH] Criando chamada - user1: {} ({}), user2: {} ({})", 
                        user1Id, user1.getName(), user2Id, user2.getName());

            CallEntity call = new CallEntity();
            call.setUser1(user1);
            call.setUser2(user2);
            call.setStartedAt(LocalDateTime.now());
            call.setCallType(CallEntity.CallType.VIDEO);
            call.setStatus(CallEntity.CallStatus.ACTIVE);

            call = callTree.addCall(call);

            userInCall.put(user1Id, call.getId());
            userInCall.put(user2Id, call.getId());

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
        messagingTemplate.convertAndSendToUser(userId.toString(), destination, payload);
    }

    public Long getUserCallId(Long userId) {
        return userInCall.get(userId);
    }

    public boolean isInCall(Long userId) {
        return userInCall.containsKey(userId);
    }
}
